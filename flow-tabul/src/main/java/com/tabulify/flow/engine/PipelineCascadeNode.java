package com.tabulify.flow.engine;

import com.tabulify.flow.FlowLog;
import com.tabulify.flow.operation.PipelineStepProcessingType;
import com.tabulify.spi.DataPath;
import com.tabulify.exception.ExceptionWrapper;
import com.tabulify.exception.InternalException;
import com.tabulify.type.time.DurationShort;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * A cascade of {@link Pipeline}
 * A pipeline with a {@link PipelineStepIntermediateSupplier steps}
 * is logically 2 pipelines
 */
public class PipelineCascadeNode {


  /**
   * Prefix of the error message
   */
  public static final String ERROR_IN_PREFIX = "Error in";
  /**
   * The root supplier (the first supplier)
   */
  private final PipelineStepRoot rootSupplierStep;
  /**
   * The intermediate supplier (the supplier in the cascade)
   */
  private final PipelineStepIntermediateSupplier intermediateSupplierStep;
  /**
   * The map operations
   */
  private final List<PipelineStepIntermediateMap> mapStepList = new ArrayList<>();


  /**
   * The child pipeline if any
   */
  private PipelineCascadeNode childPipelineNode = null;


  /**
   * The executor of
   */
  private ScheduledExecutorService supplierFromCollectorExecutor = null;
  private ScheduledFuture<?> collectorFuture = null;


  /**
   * The last poll time in millisecond
   */
  private long lastPollTimeMs;

  /**
   * @param dataPath - null if root node otherwise the data path of the parent pipeline node
   */
  public void execute(DataPath dataPath) {

    if (this.rootSupplierStep != null) {
      this.executeSupplier(rootSupplierStep);
      return;
    }

    assert this.intermediateSupplierStep != null : "Root and child supplier should not be null together";

    /**
     * Split
     */
    if (intermediateSupplierStep instanceof PipelineStepIntermediateOneToMany) {
      PipelineStepSupplierDataPath intermediateSupplier = ((PipelineStepIntermediateOneToMany) this.intermediateSupplierStep).apply(dataPath);
      this.getPipeline().getPipelineResult().addStepResult(this.intermediateSupplierStep, dataPath, PipelineStepResultDirection.IN);
      intermediateSupplier.onStart();
      try {
        this.executeSupplier(intermediateSupplier);
      } finally {
        intermediateSupplier.onComplete();
      }
      return;
    }

    /**
     * Collector
     */
    if (intermediateSupplierStep instanceof PipelineStepIntermediateManyToManyAbs) {
      this.getPipeline().getPipelineResult().addStepResult(this.intermediateSupplierStep, dataPath, PipelineStepResultDirection.IN);
      try {
        ((PipelineStepIntermediateManyToManyAbs) this.intermediateSupplierStep).accept(dataPath);
      } catch (Exception e) {
        errorHandling(e, this.intermediateSupplierStep, "", dataPath);
      }
      return;
    }

    throw new InternalException("The intermediate supplier " + intermediateSupplierStep.getClass().getSimpleName() + " was not taken into account");

  }


  /**
   * @param throwable        - the exception or the cause
   * @param executionNode    - the node (pipeline or step)
   * @param exceptionContext - an extra description
   * @param dataPath         - the data path cause or null if unknown (ie collector get error)
   */
  private void errorHandling(Throwable throwable, ExecutionNode executionNode, String exceptionContext, DataPath dataPath) {
    /**
     * Error may be reported on  {@link Pipeline}
     * but the error actions applies only to step
     */
    if (executionNode instanceof PipelineStep) {
      PipelineStep pipelineStep = (PipelineStep) executionNode;
      this.getPipeline().getPipelineResult().addError(pipelineStep);
      switch (this.getPipeline().getErrorAction()) {
        case DISCARD:
          return;
        case PARK:
          List<DataPath> dataPathsToPark = new ArrayList<>();
          if (dataPath != null) {
            dataPathsToPark.add(dataPath);
          }
          if (executionNode instanceof PipelineStepIntermediateManyToManyAbs) {
            PipelineStepIntermediateManyToManyAbs collection = (PipelineStepIntermediateManyToManyAbs) executionNode;
            collection.getDataPathsBuffer();
          }

          for (DataPath dataPathToPark : dataPathsToPark) {
            DataPath targetDataPath;
            try {
              /**
               * The target data path is a directory
               * In the {@link TransferManagerOrder order} below, if the resource is a sql or memory object, the final file name gets the {@link com.tabulify.fs.FsConnectionAttribute#TABULAR_FILE_TYPE} type
               */
              targetDataPath = this.getPipeline().getParkingTargetUriFunction().apply(dataPathToPark);
            } catch (Exception e) {
              throw new IllegalArgumentException("An error occurred and we couldn't park the implicated resource (" + dataPathToPark + "). Why? we couldn't calculate the parking target. \nError: " + e.getMessage()
                + "\nOriginal Error Message: " + throwable.getMessage(), throwable);
            }
            try {
              this.getPipeline().getParkingTransferManager().createOrder(dataPathToPark, targetDataPath).execute();
              this.getPipeline().getPipelineResult().addParking(pipelineStep, targetDataPath);
            } catch (Exception e) {
              throw new IllegalArgumentException("An error occurred and we stopped the pipeline because we couldn't park the implicated resource (" + dataPathToPark + ") to the parking (" + targetDataPath + "). Error: \n" + e.getMessage() + "\nOriginal Error Message: " + throwable.getMessage(), e);
            }
          }
          return;
        case STOP:
          break;
        default:
          throw new InternalException("The error action " + this.getPipeline().getErrorAction() + " was not taken into account");
      }
    }
    String context = ERROR_IN_PREFIX + " the " + executionNode.getNodeType() + " (" + executionNode + "). " + exceptionContext;
    throw ExceptionWrapper.builder(throwable, context)
      .setPosition(ExceptionWrapper.ContextPosition.FIRST)
      .buildAsRuntimeException();
  }

  /**
   * Execute a supplier in a batch fashion
   * ie when {@link PipelineStepSupplierDataPath#hasNext()} is false,
   * the processing stops
   */
  private void executeSupplier(PipelineStepSupplierDataPath supplierStep) {


    /**
     * Init
     */
    PipelineStepProcessingType processingType = getPipeline().getProcessingType();


    /**
     * The run
     */
    FlowLog.LOGGER.info("Executing the step " + supplierStep);
    DataPath pipelineDataPath;
    // A variable to be sure that we don't get the same data path twice
    DataPath lastDataPath = null;
    long cycleDataPathCounter = 0;
    pipeline:
    while (true) {
      /**
       * Get
       */
      boolean hasNext = supplierStep.hasNext();

      /**
       * Max cycle is for now based on number of data resources returned
       * by default
       */
      if (hasNext) {
        /**
         * Cycle count breaker
         * Stream only but yeah
         */
        if (cycleDataPathCounter >= this.getPipeline().getMaxCycleCount()) {
          break;
        }
        /**
         * the counter++ is after the test because
         * 0 >= Long.MAX_VALUE + 1 return true
         */
        cycleDataPathCounter++;
      }

      /**
       * Processing
       */
      switch (processingType) {
        case BATCH:
          if (!hasNext) {
            break pipeline;
          }
          break;
        case STREAM:
          /**
           * Control if there was any error in the {@link #scheduleChildIntermediateCollectorNodeAtInterval()}
           */
          this.checkAndThrowIfErrorOnCollectorExecutor();
          // root supplier
          if (supplierStep instanceof PipelineStepRootStreamSupplier) {
            long waitTime;
            PipelineWaitTimeType waitTimeType = null;
            if (!hasNext) {
              DurationShort pollInterval = this.getPipeline().getPollInterval();
              if (lastPollTimeMs == 0) {
                waitTime = 0;
              } else {
                long now = System.currentTimeMillis();
                long duration = now - lastPollTimeMs;
                waitTime = pollInterval.getDuration().toMillis() - duration;
                if (waitTime < 0) {
                  waitTime = 0;
                } else {
                  waitTimeType = PipelineWaitTimeType.POLL;
                }
              }
            } else {
              waitTime = this.getPipeline().getPushInterval().getDuration().toMillis();
              waitTimeType = PipelineWaitTimeType.PUSH;
            }
            if (waitTime != 0) {
              try {
                //noinspection because "busy waiting" is another word for "polling"
                //noinspection BusyWait
                Thread.sleep(waitTime);
                /**
                 * Record the wait time
                 * We do it after because as the execution may be stopped,
                 * if we record it before, the total execution time may be less
                 * than the total wait time, which is weird
                 */
                switch (waitTimeType) {
                  case POLL:
                    this.getPipeline().getPipelineResult().addPollSleepTime(waitTime);
                    break;
                  case PUSH:
                    this.getPipeline().getPipelineResult().addPushSleepTime(waitTime);
                    break;
                  default:
                    throw new InternalException("The wait time " + waitTimeType + " was not processed");
                }
              } catch (InterruptedException e) {
                errorHandling(e, this.getPipeline(), "The wait poll/push time was interrupted", null);
              }
            }
            if (!hasNext) {
              lastPollTimeMs = System.currentTimeMillis();
              this.getPipeline().getPipelineResult().addStepExecution(this.getSupplier());
              ((PipelineStepRootStreamSupplier) supplierStep).poll();
              continue;
            }
          } else {
            // intermediate supplier are not stream
            if (!hasNext) {
              break pipeline;
            }
          }
          break;
        default:
          throw new InternalException("The processing type " + processingType + " was not processed");
      }

      pipelineDataPath = supplierStep.get();
      this.getPipeline().getPipelineResult().addStepResult(supplierStep, pipelineDataPath, PipelineStepResultDirection.OUT);

      /**
       * To avoid infinite recursion in batch
       * We checked if the same data path was sent twice
       */
      if (lastDataPath != null && lastDataPath.equals(pipelineDataPath) && this.getPipeline().getProcessingType() == PipelineStepProcessingType.BATCH) {
        /**
         * This rule is not true for split operation
         * A split operation (one to many) may send the same data path
         * Example: unzip may send the result separated
         * to pass it to the print operation in order to process
         * them one by one
         */
        PipelineStepIntermediateSupplier localIntermediateSupplier = supplierStep.getIntermediateSupplier();
        if (!(localIntermediateSupplier instanceof PipelineStepIntermediateOneToMany)) {
          throw new InternalException("The supplier (" + supplierStep + ") has produced the same data resource twice: (" + pipelineDataPath + ")");
        }
      }
      lastDataPath = pipelineDataPath;

      for (PipelineStepIntermediateMap stepMap : mapStepList) {

        /**
         * Record input
         */
        this.getPipeline().getPipelineResult().addStepResult(stepMap, pipelineDataPath, PipelineStepResultDirection.IN);
        this.getPipeline().getPipelineResult().addStepExecution(stepMap);
        /**
         * Process
         */
        try {
          pipelineDataPath = stepMap.apply(pipelineDataPath);
        } catch (Exception e) {
          errorHandling(e, stepMap, "", pipelineDataPath);
        }

        /**
         * Basic check
         * Record Output
         */
        if (pipelineDataPath == null) {
          if (!(stepMap instanceof PipelineStepIntermediateMapNullable)) {
            throw new InternalException("The map step (" + stepMap + ") should not return null");
          }
        } else {

          /**
           * Columns should be there
           */
          if (pipelineDataPath.getRelationDef() != null && pipelineDataPath.getRelationDef().getColumnsSize() == 0) {
            throw new InternalException("The step (" + stepMap + ", " + stepMap.getClass().getSimpleName() + ") has yield a data path without any columns (Data Path:" + pipelineDataPath + ")");
          }

          this.getPipeline().getPipelineResult().addStepResult(stepMap, pipelineDataPath, PipelineStepResultDirection.OUT);
        }


      }


      // End
      if (this.childPipelineNode != null) {
        this.childPipelineNode.execute(pipelineDataPath);
        /**
         * We call it here in case the pipeline is a stream
         * We execute it after we gave it at minimum once
         * In Batch, it's executed on the {@link #onComplete()}
         */
        scheduleChildIntermediateCollectorNodeAtInterval();
      } else {
        this.getPipeline().getPipelineResult().addDownStreamDataPath(pipelineDataPath);
      }

    }

    /**
     * Close the down/child pipeline node
     * Run the intermediate collector
     * * They have not run in batch
     */
    if (childPipelineNode != null && processingType == PipelineStepProcessingType.BATCH) {
      this.childPipelineNode.executeSupplierFromCollector();
    }

    /**
     * Execution is at the end because {@link PipelineStepIntermediateManyToManyAbs collector}
     * may have no data path to send
     */
    if (supplierStep != rootSupplierStep && cycleDataPathCounter != 0) {
      this.getPipeline().getPipelineResult().addStepExecution(supplierStep);
    }

  }

  /**
   * Control if there was any error in the {@link #scheduleChildIntermediateCollectorNodeAtInterval()}
   */
  private void checkAndThrowIfErrorOnCollectorExecutor() {
    if (supplierFromCollectorExecutor == null) {
      return;
    }
    /**
     * The supplier is shutdown by the scheduled function
     * if there is any error
     * The future may be cancelled when shutting down the pipeline {@link #onComplete()}
     */
    if (supplierFromCollectorExecutor.isShutdown() && !collectorFuture.isCancelled()) {
      try {
        // If the execution of the task throws an exception.
        // Calling get on the returned future will throw ExecutionException, holding the exception as its cause.
        collectorFuture.get();
      } catch (ExecutionException | InterruptedException e) {
        // The original error is in the cause
        Throwable cause = e.getCause();
        errorHandling(cause, this.childPipelineNode.getSupplier(), "", null);
      }
    }
  }

  /**
   * In a stream fashion, the intermediate collector (ie {@link PipelineStepIntermediateManyToManyAbs}
   * should be executed periodically
   */
  private void scheduleChildIntermediateCollectorNodeAtInterval() {

    PipelineCascadeNodeSupplierStep childNodeSupplier = this.childPipelineNode.getSupplier();
    if (!(childNodeSupplier instanceof PipelineStepIntermediateManyToManyAbs)) {
      return;
    }
    PipelineStepProcessingType processingType = getPipeline().getProcessingType();
    if (processingType != PipelineStepProcessingType.STREAM) {
      return;
    }
    if (supplierFromCollectorExecutor != null) {
      return;
    }
    // https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ScheduledExecutorService.html
    // https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html
    supplierFromCollectorExecutor = Executors.newSingleThreadScheduledExecutor();
    long windowInterval = getPipeline().getWindowInterval().getDuration().toMillis();

    collectorFuture = supplierFromCollectorExecutor.scheduleAtFixedRate(() -> {
        try {
          this.childPipelineNode.executeSupplierFromCollector();
        } catch (Throwable e) {
          // we shut down the executor
          // this is how we check if there was any error
          supplierFromCollectorExecutor.shutdown();
          // we throw the error, so that we can get with the collectorFuture.get
          throw e;
        }
      }
      ,
      windowInterval,
      windowInterval,
      TimeUnit.MILLISECONDS
    );


  }

  /**
   * @param rootSupplierStep         - the root supplier step
   * @param intermediateSupplierStep - A child supplier step that provides a supplier from a data path
   */
  public PipelineCascadeNode(PipelineStepRoot rootSupplierStep, PipelineStepIntermediateSupplier intermediateSupplierStep) {
    this.rootSupplierStep = rootSupplierStep;
    this.intermediateSupplierStep = intermediateSupplierStep;
    if (rootSupplierStep == null && intermediateSupplierStep == null) {
      throw new InternalException("The root supplier and the child supplier should not be both null");
    }
    if (rootSupplierStep != null && intermediateSupplierStep != null) {
      throw new InternalException("The root supplier and the child supplier should not be both provided");
    }
  }

  public boolean isParent() {
    return this.childPipelineNode != null;
  }

  public PipelineCascadeNode getChild() {
    return this.childPipelineNode;
  }

  public List<PipelineStep> getSteps() {
    List<PipelineStep> lists = new ArrayList<>();
    lists.add(this.getSupplier());
    lists.addAll(this.mapStepList);
    if (this.childPipelineNode != null) {
      lists.addAll(this.childPipelineNode.getSteps());
    }
    return lists;
  }

  /**
   * @param supplierStep - supplier step
   * @return the created child
   */
  public PipelineCascadeNode createChild(PipelineStepIntermediateSupplier supplierStep) {
    this.childPipelineNode = new PipelineCascadeNode(null, supplierStep);
    return this.childPipelineNode;
  }

  public PipelineCascadeNode addMapStep(PipelineStepIntermediateMapAbs filterStep) {
    this.mapStepList.add(filterStep);
    return this;
  }

  Pipeline getPipeline() {
    return this.getSupplier().getPipeline();
  }

  /**
   * Execution that runs on completion of the {@link #executeSupplier(PipelineStepSupplierDataPath)}
   */
  public void onComplete() {

    /**
     * A stream may complete
     * thanks to {@link Pipeline#setMaxCycleCount(int)}
     */

    /**
     * Collector execution
     * <p>
     * If the pipeline is a stream, the collector are executed periodically
     * We wait its termination otherwise, we may miss error
     * on the last step
     */
    if (supplierFromCollectorExecutor != null) {

      this.checkAndThrowIfErrorOnCollectorExecutor();
      supplierFromCollectorExecutor.shutdown();
      try {
        int timeoutSecond = 10;
        boolean terminatedNormally = supplierFromCollectorExecutor.awaitTermination(timeoutSecond, TimeUnit.SECONDS);
        if (!terminatedNormally) {
          throw new RuntimeException("The collector executor of the step " + this.childPipelineNode + " did not shutdown under " + timeoutSecond + " seconds and was terminated.");
        }
      } catch (InterruptedException e) {
        throw new RuntimeException("The collector executor of the step " + this.childPipelineNode + " was interrupted. This interruption was not expected");
      }
      /**
       * They may have not run in stream with the executor because it has been shutdown
       */
      this.childPipelineNode.executeSupplierFromCollector();

    }


    if (rootSupplierStep != null) {
      rootSupplierStep.onComplete();
    }
    for (PipelineStepIntermediateMap step : mapStepList) {
      step.onComplete();
    }
    if (childPipelineNode != null) {
      childPipelineNode.onComplete();
    }

  }

  /**
   * A collector should be executed:
   * * periodically for a stream
   * * at the end for a batch
   */
  public void executeSupplierFromCollector() {
    if (!(intermediateSupplierStep instanceof PipelineStepIntermediateManyToManyAbs)) {
      /**
       * The function is called recursively at the end of a {@link PipelineCascadeNode#execute(DataPath) node execution}
       * without regards of the node itself
       * Not an error then
       */
      return;
    }
    PipelineStepIntermediateManyToManyAbs intermediateSupplierManyToMany = (PipelineStepIntermediateManyToManyAbs) this.intermediateSupplierStep;
    PipelineStepSupplierDataPath supplier = null;
    try {
      supplier = intermediateSupplierManyToMany.get();
      intermediateSupplierManyToMany.reset();
    } catch (Exception e) {
      errorHandling(e, intermediateSupplierManyToMany, "", null);
    }
    if (supplier == null) {
      return;
    }
    this.executeSupplier(supplier);
  }


  @Override
  public String toString() {
    if (rootSupplierStep != null) {
      return rootSupplierStep.toString();
    }
    return intermediateSupplierStep.toString();
  }


  public PipelineStepProcessingType getProcessingType() {
    if (rootSupplierStep != null) {
      return rootSupplierStep.getProcessingType();
    }
    return getSupplier().getPipeline().getProcessingType();
  }

  public PipelineCascadeNodeSupplierStep getSupplier() {
    if (rootSupplierStep != null) {
      return rootSupplierStep;
    }
    return intermediateSupplierStep;
  }

  public void onStart() {

    if (rootSupplierStep != null) {
      rootSupplierStep.onStart();
    }
    for (PipelineStepIntermediateMap step : mapStepList) {
      step.onStart();
    }
    if (intermediateSupplierStep != null) {
      intermediateSupplierStep.onStart();
    }
    if (childPipelineNode != null) {
      childPipelineNode.onStart();
    }
  }
}
