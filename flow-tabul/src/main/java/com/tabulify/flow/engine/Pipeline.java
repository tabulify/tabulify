package com.tabulify.flow.engine;


import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.conf.ManifestDocument;
import com.tabulify.flow.FlowLog;
import com.tabulify.flow.fs.PipelineMediaType;
import com.tabulify.flow.operation.PipelineStepProcessingType;
import com.tabulify.template.TemplateUriFunction;
import com.tabulify.transfer.TransferManager;
import com.tabulify.exception.CastException;
import com.tabulify.exception.ExceptionWrapper;
import com.tabulify.exception.IllegalArgumentExceptions;
import com.tabulify.exception.InternalException;
import com.tabulify.type.Casts;
import com.tabulify.type.KeyCase;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.time.DurationShort;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.tabulify.flow.engine.PipelineStepAttribute.OPERATION;

/**
 * A pipeline is a list  of {@link PipelineStep} that:
 * * starts from a {@link PipelineStepRoot}
 * * and goes through {@link PipelineStepIntermediate}
 * <p>
 * Because a {@link PipelineStepIntermediate} may also produce a {@link PipelineStepSupplierDataPath} (ie {@link PipelineStepIntermediateSupplier}),
 * a pipeline is modeled as a chain of {@link PipelineCascadeNode}.
 * <p>
 * A {@link PipelineCascadeNode} is composed of
 * * a {@link PipelineCascadeNodeSupplierStep}
 * * one or many {@link PipelineStepIntermediateMap}
 * * and an optional {@link PipelineCascadeNode consumer node}
 *
 * <p>
 * Vocabulary:
 * * a supplier is a {@link PipelineCascadeNodeSupplierStep} that returns or is a {@link PipelineStepSupplierDataPath} (DataPath get())
 * * the first operation in the pipeline is called a {@link PipelineStepRoot root}
 * * the step below the root are called {@link PipelineStepIntermediate intermediate}
 * * there is no terminal operation
 */

public class Pipeline extends ExecutionNodeAbs implements ExecutionNode {


  private final PipelineBuilder pipelineBuilder;


  /**
   * A result object that encapsulate the result
   */
  private PipelineResult pipelineResult;
  /**
   * An execution counter of this pipeline
   */
  private int executionCounter = 0;
  private int exitStatus = 0;


  public static Pipeline createFromYamlPath(Tabular tabular, Path path) {


    FlowLog.LOGGER.fine("Parsing the flow file (" + path + ")");

    PipelineBuilder pipelineBuilder = builder(tabular)
      .setScriptPath(path);

    ManifestDocument manifestDocument = ManifestDocument.builder()
      .setPath(path)
      .build();

    KeyNormalizer kind = manifestDocument.getKind();
    if (!kind.equals(PipelineMediaType.KIND)) {
      throw new IllegalArgumentException("The metadata document (" + manifestDocument + ") is not a pipeline but a " + kind);
    }

    /**
     * Process Pipeline attributes
     *
     */
    List<Object> pipelineStepList = null;
    for (Map.Entry<KeyNormalizer, Object> pipelineAttributeEntry : manifestDocument.getSpecMap().entrySet()) {
      PipelineFileAttribute pipelineAttribute;
      KeyNormalizer pipelineAttributeKeyNormalized = pipelineAttributeEntry.getKey();
      Object pipelineAttributeValue = pipelineAttributeEntry.getValue();
      try {
        pipelineAttribute = PipelineFileAttribute.cast(pipelineAttributeKeyNormalized);
      } catch (CastException e) {
        String expectedKeyAsString = Arrays.stream(PipelineFileAttribute.class.getEnumConstants())
          .sorted()
          .map(c -> KeyNormalizer.createSafe(c).toCliLongOptionName())
          .collect(Collectors.joining(", "));
        throw new IllegalArgumentException("The pipeline attribute (" + pipelineAttributeKeyNormalized + ") is unknown. We were expecting one of: " + expectedKeyAsString);
      }

      switch (pipelineAttribute) {
        case COMMENT:
          pipelineBuilder.setComment((String) pipelineAttributeValue);
          break;
        case STEPS:
          try {
            pipelineStepList = Casts.castToNewList(pipelineAttributeValue, Object.class);
          } catch (CastException e) {
            throw new IllegalArgumentException("The pipeline value is not a map but a " + pipelineAttributeValue.getClass().getSimpleName(), e);
          }
          break;
        case ARGUMENTS:
          Map<String, Object> argumentObjectMap;
          try {
            argumentObjectMap = Casts.castToSameMap(pipelineAttributeValue, String.class, Object.class);
          } catch (CastException e) {
            throw new IllegalArgumentException("The pipeline attribute (" + pipelineAttributeKeyNormalized + ") is not a map but a " + pipelineAttributeValue.getClass().getSimpleName());
          }

          Map<KeyNormalizer, Object> argumentsMap = new HashMap<>();
          for (Map.Entry<String, Object> argumentEntry : argumentObjectMap.entrySet()) {
            String pipelineArgumentString = argumentEntry.getKey();
            try {
              argumentsMap.put(KeyNormalizer.create(pipelineArgumentString), argumentEntry.getValue());
            } catch (CastException e) {
              throw new IllegalArgumentException("The pipeline argument key (" + pipelineArgumentString + ") is not valid. Error: " + e.getMessage(), e);
            }
          }
          pipelineBuilder.setArguments(argumentsMap);
          break;

        default:
          throw new InternalException("The pipeline attribute (" + pipelineAttribute + ") was not processed");
      }

    }

    if (pipelineStepList == null) {
      throw new IllegalArgumentException("The `" + PipelineFileAttribute.STEPS + "` attribute could not be found");
    }
    if (pipelineStepList.isEmpty()) {
      throw new IllegalArgumentException("The `" + PipelineFileAttribute.STEPS + "` attribute is an empty list. No step to add.");
    }



    /*
     * The known operation
     */
    List<PipelineStepBuilder> stepBuilderRegistered = PipelineStepBuilder.installedProviders();

    /**
     * Add the steps
     * Loop through the steps (ie list of maps)
     */
    int stepCounter = 0;

    for (Object stepObject : pipelineStepList) {

      // Init
      stepCounter++;
      String stepName = "step" + stepCounter;
      String stepDescription = null;
      KeyNormalizer operation = null;
      Map<KeyNormalizer, Object> arguments = new HashMap<>();

      // Loop over the step attribute
      Map<String, Object> stepMap;
      try {
        stepMap = Casts.castToSameMap(stepObject, String.class, Object.class);
      } catch (CastException e) {
        throw new InternalException("String and Object should not throw a cast exception", e);
      }
      for (Map.Entry<String, Object> stepEntry : stepMap.entrySet()) {
        String key = stepEntry.getKey();
        Object value = stepEntry.getValue();
        if (value == null) {
          throw new IllegalArgumentException("The attribute (" + key + ") on the step (" + stepName + ") has a null value");
        }
        PipelineStepAttribute stepAttribute;
        try {
          stepAttribute = PipelineStepAttribute.cast(key);
        } catch (CastException e) {
          throw IllegalArgumentExceptions.createForStepArgument(key, stepName, PipelineStepAttribute.class, e);
        }
        switch (stepAttribute) {
          case NAME:
            stepName = value.toString();
            break;
          case OPERATION:
            try {
              operation = KeyNormalizer.create(value);
            } catch (CastException e) {
              throw new IllegalArgumentException("The operation name (" + value + ") is not conform. Error: " + e.getMessage(), e);
            }
            break;
          case COMMENT:
            stepDescription = value.toString();
            break;
          case ARGUMENTS:
            if (!(value instanceof Map)) {
              throw new IllegalArgumentException("The arguments of the step (" + stepName + ") are not a list of key-pair values (ie a map) but a " + value.getClass().getSimpleName());
            }
            try {
              arguments = Casts.castToNewMap(value, KeyNormalizer.class, Object.class);
            } catch (CastException e) {
              throw new IllegalArgumentException("The arguments map is not valid for the step (" + stepName + "). Error: " + e.getMessage(), e);
            }
            break;
          default:
            throw new InternalException("Error while reading the pipeline file, the step attribute (" + stepAttribute + ") should have a branch");
        }

      }

      if (operation == null) {
        throw new IllegalArgumentException("We were unable to find the (" + KeyNormalizer.createSafe(OPERATION).toCliLongOptionName() + ") argument in the " + stepName + " step (Data: " + stepMap + ")");
      }


      PipelineStepBuilder stepBuilder = null;
      for (PipelineStepBuilder registeredStepBuilder : stepBuilderRegistered) {
        if (registeredStepBuilder.acceptOperation(operation)) {
          stepBuilder = registeredStepBuilder.createStepBuilder();
        }
      }
      if (stepBuilder == null) {
        List<KeyNormalizer> registeredNames = stepBuilderRegistered
          .stream()
          .flatMap(opRegistered -> opRegistered.getAcceptedCommandNames().stream())
          .sorted()
          .distinct()
          .collect(Collectors.toList());
        String availableOperationNames = registeredNames.stream().map(n -> n.toCase(KeyCase.KEBAB)).collect(Collectors.joining(", "));
        throw new IllegalArgumentException("The operation (" + operation + ") is unknown or unregistered for the step (" + stepName + ") step. The available operations are (" + availableOperationNames + ").");
      }

      // Injection
      stepBuilder.setAcceptedOperation(operation);
      stepBuilder.setPipelineBuilder(pipelineBuilder);

      try {
        stepBuilder.setNodeName(stepName);
      } catch (RuntimeException e) {
        throw new RuntimeException("The step name (`" + stepName + "`) is not compliant: " + e.getMessage());
      }
      if (stepDescription != null) {
        stepBuilder.setComment(stepDescription);
      }

      if (!arguments.isEmpty()) {
        stepBuilder.setArguments(arguments);
      }

      FlowLog.LOGGER.fine("Step `" + stepName + "` with the operation " + operation + " found");

      pipelineBuilder.addStep(stepBuilder);

    }
    return pipelineBuilder.build();
  }


  public Pipeline(PipelineBuilder pipelineBuilder) {
    super(pipelineBuilder);
    this.pipelineBuilder = pipelineBuilder;
  }

  public static PipelineBuilder builder(Tabular tabular) {
    return new PipelineBuilder()
      .setTabular(tabular);
  }

  /**
   * The pipeline execution unit
   */
  public PipelineResult execute() {

    /**
     * We may execute the same multiple time
     */
    if (executionCounter != 0) {
      // rebuilt pipelineBuilder.pipelineRootNode
      this.pipelineBuilder.buildOrRebuildCascadeFromSteps(this);
    }
    executionCounter++;

    /**
     * Run
     */
    this.pipelineResult = new PipelineResult(this);
    this.pipelineResult.start();

    /**
     * Execution with timeout or not
     */
    try {
      DurationShort timeOut = this.getTimeout();
      if (timeOut != null) {
        /**
         * Timeout execution
         */
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
          Future<?> future = executor.submit(() -> this.pipelineBuilder.pipelineRootNode.execute(null));
          long millis = timeOut.getDuration().toMillis();
          this.pipelineResult.startExecutionTimer();
          future.get(millis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
          if (this.pipelineBuilder.getTimeOutType() == PipelineTimeoutType.ERROR) {
            throw ExceptionWrapper.builder(e, "The pipeline (" + this + ") timed out after " + timeOut + ".")
              .setPosition(ExceptionWrapper.ContextPosition.FIRST)
              .buildAsRuntimeException();
          }

          /**
           * Shutdown takes also some time
           * This is why the execution timer stop is not here
           */
          this.pipelineResult.endExecutionTimer();

        } catch (ExecutionException e) {
          // the original error is in the cause
          Throwable cause = e.getCause();
          throw ExceptionWrapper.builder(cause, "The pipeline (" + this + ") had an unexpected error. Error:  " + cause.getMessage())
            .setPosition(ExceptionWrapper.ContextPosition.FIRST)
            .buildAsRuntimeException();
        } catch (InterruptedException e) {
          throw ExceptionWrapper.builder(e, "The pipeline (" + this + ") had an unexpected interruption error. Error:  " + e.getCause())
            .setPosition(ExceptionWrapper.ContextPosition.FIRST)
            .buildAsRuntimeException();
        } finally {

          executor.shutdown();
          try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
              executor.shutdownNow();
            }
          } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
          }
        }
      } else {

        this.pipelineResult.startExecutionTimer();
        this.pipelineBuilder.pipelineRootNode.onStart();
        this.pipelineBuilder.pipelineRootNode.execute(null);
        this.pipelineResult.endExecutionTimer();

      }

    } finally {

      this.pipelineBuilder.pipelineRootNode.onComplete();

    }
    this.pipelineResult.stop();
    return this.pipelineResult;
  }


  public List<PipelineStep> getSteps() {
    return this.pipelineBuilder.pipelineRootNode.getSteps();
  }


  public int size() {
    return getSteps().size();
  }


  @Override
  public Integer getNodeId() {
    return 0;
  }

  @Override
  public KeyNormalizer getNodeName() {

    return this.pipelineBuilder.getNodeName();
  }

  @Override
  public List<Attribute> getArguments() {
    return this.pipelineBuilder.getArguments();
  }

  public Tabular getTabular() {
    return this.pipelineBuilder.getTabular();
  }

  @Override
  public String getNodeType() {
    return "Pipeline";
  }

  public boolean isStrict() {
    Boolean strict = this.pipelineBuilder.isStrict();
    if (strict != null) {
      return strict;
    }
    return getTabular().isStrictExecution();
  }

  public PipelineStepProcessingType getProcessingType() {
    return this.pipelineBuilder.pipelineRootNode.getProcessingType();
  }

  @Override
  public String toString() {
    return getNodeName().toString();
  }


  public long getMaxCycleCount() {
    return this.pipelineBuilder.getMaxCycleCount();
  }


  /**
   * The pooling interval in milliseconds when no data path is supplied
   * <p>
   * Example:
   * * Local file system operations: 100-500ms (File creation, deletion, or modification checks, Log file monitoring)
   * * Database polling: 1-5 seconds (Checking for new records, Job queue processing, Status updates)
   * * External API polling: 5-30 seconds (REST API status checks, Third-party service integration, Rate-limited services)
   * * Long-running processes: 30 seconds - 5 minutes (Batch job completion, System maintenance tasks, Background data processing)
   */
  public DurationShort getPollInterval() {
    return this.pipelineBuilder.getPollInterval();
  }

  /**
   * The wait interval between get in milliseconds
   * <p>
   * If zero, no wait
   * This is used to slow down the stream.
   * For instance, a supplier of generated time is almost instantaneous, you may
   * want to get it only every seconds
   */
  public DurationShort getPushInterval() {
    return this.pipelineBuilder.getPushInterval();
  }


  /**
   * The time in milliseconds of window computation
   * (ie The period in time where the {@link PipelineStepIntermediateManyToManyAbs}
   * step are executed)
   */
  public DurationShort getWindowInterval() {
    return this.pipelineBuilder.getWindowInterval();
  }

  /**
   * @return the maximum sleep interval
   * (ie the max between the {@link #getPushInterval()}
   * and {@link #getPollInterval()}
   */
  @SuppressWarnings("unused")
  public DurationShort getMaxSleepInterval() {
    if (getPushInterval().getDuration().compareTo(getPollInterval().getDuration()) > 0) {
      return getPushInterval();
    }
    return getPollInterval();
  }

  public boolean getIsDownStreamDataPathCollected() {
    return this.pipelineBuilder.isDownStreamDataPathCollected;
  }


  public PipelineResult getPipelineResult() {
    return this.pipelineResult;
  }

  public DurationShort getTimeout() {
    return this.pipelineBuilder.getTimeOut();
  }

  public PipelineTimeoutType getTimeoutType() {
    return this.pipelineBuilder.getTimeOutType();
  }


  public Path getScriptPath() {
    return this.pipelineBuilder.getScriptPath();
  }

  public PipelineOnErrorAction getErrorAction() {
    return this.pipelineBuilder.errorAction;
  }


  public TemplateUriFunction getParkingTargetUriFunction() {
    return this.pipelineBuilder.parkingSourceTargetFunction;
  }


  public TransferManager getParkingTransferManager() {
    return this.pipelineBuilder.parkingTransferManager;
  }

  /**
   * Failure may have been seen in parallel process
   * or the process may continue (ie exec will execute all of them and report an error at the end if any occurs)
   * The failure may be reported here
   */
  public void setExitStatus(int i) {
    this.exitStatus = i;
    //noinspection resource
    this.getTabular().setExitStatus(i);
  }

  public int getExitStatus() {
    return this.exitStatus;
  }

}
