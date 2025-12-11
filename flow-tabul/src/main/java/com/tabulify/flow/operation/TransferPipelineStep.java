package com.tabulify.flow.operation;

import com.tabulify.DbLoggers;
import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.flow.engine.PipelineStep;
import com.tabulify.flow.engine.PipelineStepBuilderTarget;
import com.tabulify.flow.engine.PipelineStepBuilderTargetArgument;
import com.tabulify.spi.DataPath;
import com.tabulify.template.TemplateUriFunction;
import com.tabulify.transfer.*;
import com.tabulify.uri.DataUriNode;
import com.tabulify.exception.CastException;
import com.tabulify.exception.InternalException;
import com.tabulify.type.Casts;
import com.tabulify.type.Enums;
import com.tabulify.type.KeyNormalizer;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.tabulify.flow.operation.StepOutputArgument.TARGETS;
import static com.tabulify.flow.operation.TransferPipelineStepArgument.*;

public class TransferPipelineStep extends PipelineStepBuilderTarget {

  public static final KeyNormalizer TRANSFER = KeyNormalizer.createSafe("transfer");
  /**
   * Named accepted as operation
   */
  public static final Set<KeyNormalizer> acceptedNames;

  protected TransferPropertiesCross transferPropertiesCross = TransferPropertiesCross.create();
  protected TransferPropertiesSystem.TransferPropertiesSystemBuilder transferPropertiesSystemBuilder = TransferPropertiesSystem.builder();

  static {

    acceptedNames = Arrays.stream(TransferOperation.values())
      .map(KeyNormalizer::createSafe)
      .collect(Collectors.toSet());
    acceptedNames.add(TRANSFER);

  }

  private StepOutputArgument transferOutput = TARGETS;
  private PipelineStepProcessingType processingType;

  private TransferManager transferManager;
  private TemplateUriFunction templateUriFunction;
  private Consumer<TransferListener> listenerConsumer;

  public static TransferPipelineStep builder() {
    return new TransferPipelineStep();
  }

  @Override
  public TransferPipelineStep createStepBuilder() {
    return new TransferPipelineStep();
  }

  @Override
  public PipelineStep build() {

    if (this.getTargetUriFunction() == null) {
      throw new IllegalArgumentException("The target uri argument (" + PipelineStepBuilderTargetArgument.TARGET_DATA_URI + ") is mandatory");
    }

    // only one target ?
    boolean isConcat = this.getTargetTemplate().getVariables().isEmpty();

    /**
     * Default operation
     * Check load operation
     * The load operation is important during the check of target structure
     * Why? a {@link TransferOperation#COPY} operation requires
     * the same data structure between the target and the source
     */
    if (transferPropertiesSystemBuilder.getOperation() == null) {
      if (isConcat) {
        transferPropertiesSystemBuilder.setOperation(TransferOperation.INSERT);
      } else {
        transferPropertiesSystemBuilder.setOperation(TransferOperation.COPY);
      }
    }

    /**
     * Building the transferManager object
     */
    transferManager = TransferManager
      .builder()
      .setTransferCrossProperties(transferPropertiesCross)
      .setTransferPropertiesSystem(transferPropertiesSystemBuilder)
      .build();

    /**
     * Building the source target helper
     */
    templateUriFunction = getTargetUriFunction();

    if (processingType == null) {

      if (isConcat) {
        // only one target (ie concat), we want to gather the sources
        // so that we can run the load in parallel
        processingType = PipelineStepProcessingType.BATCH;
      } else {
        processingType = PipelineStepProcessingType.STREAM;
      }

    }
    switch (processingType) {
      case STREAM:
        return new TransferPipelineStepStream(this);
      case BATCH:
        return new TransferPipelineStepBatch(this);
      default:
        throw new IllegalArgumentException("The processing " + processingType + " was not processed in the switch");
    }

  }


  public TransferPipelineStep setTransferCrossProperties(TransferPropertiesCross transferPropertiesCross) {
    this.transferPropertiesCross = transferPropertiesCross;
    return this;
  }

  public TransferPipelineStep setTargetDataUri(DataUriNode targetUri) {
    super.setTargetDataUri(targetUri);
    return this;
  }

  public TransferPipelineStep setOutput(StepOutputArgument attribute) {
    this.transferOutput = attribute;
    return this;
  }

  @Override
  public Set<KeyNormalizer> getAcceptedCommandNames() {
    return acceptedNames;
  }

  @Override
  public KeyNormalizer getOperationName() {
    return TRANSFER;
  }

  @Override
  public List<Class<? extends ArgumentEnum>> getArgumentEnums() {
    List<Class<? extends ArgumentEnum>> lists = new ArrayList<>(super.getArgumentEnums());
    lists.add(TransferPipelineStepArgument.class);
    return lists;
  }

  @Override
  public PipelineStepBuilderTarget setArgument(KeyNormalizer key, Object value) {

    TransferPipelineStepArgument transferPipelineStepArgument;
    try {
      transferPipelineStepArgument = Casts.cast(key.toString(), TransferPipelineStepArgument.class);
    } catch (CastException e) {
      super.setArgument(key, value);
      return this;
    }

    Attribute attribute;
    try {
      attribute = this.getTabular().getVault()
        .createVariableBuilderFromAttribute(transferPipelineStepArgument)
        .setOrigin(Origin.PIPELINE)
        .build(value);
      this.setArgument(attribute);
    } catch (CastException e) {
      throw new IllegalArgumentException("The " + transferPipelineStepArgument + " value (" + value + ") of the step (" + this + ") is not conform . Error: " + e.getMessage(), e);
    }

    switch (transferPipelineStepArgument) {

      case TRANSFER_OPERATION:
        transferPropertiesSystemBuilder.setOperation(TransferOperation.createFrom(value.toString()));
        break;
      case TRANSFER_UPSERT_TYPE:
        transferPropertiesSystemBuilder.setUpsertType((UpsertType) attribute.getValueOrDefault());
        break;
      case TRANSFER_MAPPING_STRICT:
        try {
          transferPropertiesSystemBuilder.setStrictMapping(Casts.cast(value.toString(), Boolean.class));
        } catch (CastException e) {
          throw new IllegalArgumentException("The value (" + value + ") is not a valid boolean value for the argument " + transferPipelineStepArgument + " on the step (" + this + ").");
        }
        break;
      case TRANSFER_MAPPING_METHOD:
        TransferMappingMethod transferMappingMethod;
        try {
          transferMappingMethod = Casts.cast(value.toString(), TransferMappingMethod.class);
        } catch (CastException e) {
          throw new IllegalArgumentException("The value (" + value + ") is not a valid value for the argument " + TRANSFER_MAPPING_METHOD + " on the step (" + this + "). You can enter of the following values: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TransferMappingMethod.class));
        }
        switch (transferMappingMethod) {
          case NAME:
            transferPropertiesSystemBuilder.setColumnMappingByName();
            break;
          case POSITION:
            transferPropertiesSystemBuilder.setColumnMappingByPosition();
            break;
          case MAP_BY_NAME:
          case MAP_BY_POSITION:
            // map is given in the other variable
            break;
          default:
            throw new InternalException("The method (" + transferMappingMethod + ") has not been implemented");
        }
        break;
      case TRANSFER_MAPPING_COLUMNS:
        if (!(value instanceof Map)) {
          throw new IllegalArgumentException("The mapping column value (" + value + ") is not a map but a " + value.getClass().getSimpleName());
        }
        try {
          Map<String, String> mapByName = Casts.castToSameMap(value, String.class, String.class);
          transferPropertiesSystemBuilder.setColumnMappingByNamedMap(mapByName);
        } catch (Exception e) {
          try {
            Map<Integer, Integer> mapByPosition = Casts.castToSameMap(value, Integer.class, Integer.class);
            transferPropertiesSystemBuilder.setColumnMappingByPositionalMap(mapByPosition);
          } catch (Exception ex) {
            throw new IllegalArgumentException("The mapping column value should be a map of name or a name of number. This value (" + value + ") is not");
          }
        }
        break;
      case TARGET_OPERATION:
        if (value instanceof String) {
          try {
            transferPropertiesSystemBuilder.setTargetOperations(Casts.cast(value.toString(), TransferResourceOperations.class));
          } catch (CastException e) {
            throw new IllegalArgumentException("The value (" + value + ") is not a valid transfer operation for the argument " + TARGET_OPERATION + " for the step (" + this + "). You can enter one of the following values: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TransferResourceOperations.class));
          }
        } else if (value instanceof List) {
          Collection<Object> values = Casts.castToNewListSafe(value, Object.class);
          for (Object valueEl : values) {
            try {
              transferPropertiesSystemBuilder.setTargetOperations(Casts.cast(valueEl.toString(), TransferResourceOperations.class));
            } catch (CastException e) {
              throw new IllegalArgumentException("The value (" + valueEl + ") is not a valid transfer operation for the argument " + TARGET_OPERATION + " for the step (" + this + "). You can enter one of the following values: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TransferResourceOperations.class));
            }
          }
        } else {
          throw new IllegalArgumentException("The property `" + TARGET_OPERATION + "` of the step (" + this + ") should be a list or a string but is a " + value.getClass().getSimpleName());
        }
        break;
      case SOURCE_OPERATION:
        if (value instanceof String) {
          try {
            transferPropertiesSystemBuilder.setSourceOperations(Casts.cast(value, TransferResourceOperations.class));
          } catch (CastException e) {
            throw new IllegalArgumentException("The argument (" + value + ") is not a valid transfer operation for the argument " + SOURCE_OPERATION + " for the step (" + this + "). You can enter one of the following values: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TransferResourceOperations.class));
          }
        } else if (value instanceof List) {
          TransferResourceOperations[] sourceOperations = Casts.castToNewListSafe(value, Object.class).stream().map(op -> {
              try {
                return Casts.cast(op, TransferResourceOperations.class);
              } catch (CastException e) {
                throw new IllegalArgumentException("The argument (" + op + ") is not a valid transfer operation for the argument " + SOURCE_OPERATION + " for the step (" + this + "). You can enter one of the following values: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TransferResourceOperations.class));
              }
            })
            .toArray(TransferResourceOperations[]::new);
          transferPropertiesSystemBuilder.setSourceOperations(sourceOperations);
        } else {
          throw new IllegalArgumentException("The property `" + SOURCE_OPERATION + "` of the step (" + this + ") should be a list or a string but is a " + value.getClass().getSimpleName());
        }
        break;
      case SOURCE_FETCH_SIZE:
        try {
          transferPropertiesCross.setFetchSize(Casts.cast(value, Integer.class));
        } catch (CastException e) {
          throw new IllegalArgumentException("The value (" + value + ") is not a valid integer for the argument " + SOURCE_FETCH_SIZE + " on the step (" + this + ").");
        }
        break;
      case BUFFER_SIZE:
        try {
          transferPropertiesCross.setBufferSize(Casts.cast(value, Integer.class));
        } catch (CastException e) {
          throw new IllegalArgumentException("The value (" + value + ") is not a valid integer for the argument " + BUFFER_SIZE + " on the step (" + this + ").");
        }
        break;
      case TARGET_BATCH_SIZE:
        try {
          transferPropertiesCross.setBatchSize(Casts.cast(value, Integer.class));
        } catch (CastException e) {
          throw new IllegalArgumentException("The value (" + value + ") is not a valid integer for the argument " + TARGET_BATCH_SIZE + " on the step (" + this + ").");
        }
        break;
      case TARGET_COMMIT_FREQUENCY:
        try {
          transferPropertiesCross.setCommitFrequency(Casts.cast(value, Integer.class));
        } catch (CastException e) {
          throw new IllegalArgumentException("The value (" + value + ") is not a valid integer for the argument " + TARGET_COMMIT_FREQUENCY + " on the step (" + this + ").");
        }
        break;
      case TARGET_WORKER_COUNT:
        try {
          transferPropertiesCross.setTargetWorkerCount(Casts.cast(value, Integer.class));
        } catch (CastException e) {
          throw new IllegalArgumentException("The value (" + value + ") is not a valid integer for the argument " + TARGET_WORKER_COUNT + " on the step (" + this + ").");
        }
        break;
      case WITH_PARAMETERS:
        try {
          transferPropertiesSystemBuilder.setWithParameters(Casts.cast(value, Boolean.class));
        } catch (CastException e) {
          throw new IllegalArgumentException("The value (" + value + ") is not a valid bo0lean for the argument " + WITH_PARAMETERS + " on the step (" + this + ").");
        }
        break;
      case METRICS_DATA_URI:
        DataPath metricsDataPath = this.getPipeline().getTabular().getDataPath(value.toString());
        transferPropertiesCross.setMetricsDataPath(metricsDataPath);
        break;
      case OUTPUT_TYPE:
        StepOutputArgument transferOutput;
        try {
          transferOutput = Casts.cast(value.toString(), StepOutputArgument.class);
        } catch (CastException e) {
          throw new IllegalArgumentException("The value (" + value + ") is not a valid value for the argument " + OUTPUT_TYPE + " for the step (" + this + "). You can enter one of the following values: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(StepOutputArgument.class));
        }
        this.setOutput(transferOutput);
        break;
      case PROCESSING_TYPE:
        this.setProcessingType((PipelineStepProcessingType) attribute.getValueOrDefault());
        break;
      default:
        throw new InternalException("The transfer argument (" + transferPipelineStepArgument + ") was not processed");
    }
    return this;
  }

  public TransferPipelineStep setTransferPropertiesSystem(TransferPropertiesSystem.TransferPropertiesSystemBuilder transferPropertiesSystemBuilder) {
    this.transferPropertiesSystemBuilder = transferPropertiesSystemBuilder;
    return this;
  }

  public TransferPipelineStep setProcessingType(PipelineStepProcessingType processType) {
    this.processingType = processType;
    return this;
  }

  public StepOutputArgument getTransferOutput() {
    return this.transferOutput;
  }

  List<DataPath> apply(List<DataPath> sourceDataPathList) {

    /**
     * We use new Array List to create a defensive copy before streaming
     * Why ? because we got this error:
     * java.util.ConcurrentModificationException:
     * Error in the Collector (step3 (transfer)).
     *         at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1661)
     *         at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:484)
     * Why?
     * That's because there is a {@link TransferPipelineStepBatch#reset()} ??
     */
    List<TransferSourceTarget> sourceTargets = new ArrayList<>(sourceDataPathList)
      .stream()
      .map(s -> {
        DataPath apply = templateUriFunction.apply(s);
        return TransferSourceTarget.create(s, apply);
      })
      .collect(Collectors.toList());

    /**
     * Start
     */
    List<TransferListener> transferListeners = transferManager
      .createOrder(sourceTargets)
      .execute()
      .getTransferListeners();

    if (listenerConsumer != null) {
      for (TransferListener transferListener : transferListeners) {
        listenerConsumer.accept(transferListener);
      }
    }


    // Exit
    long exitStatus = transferListeners
      .stream()
      .mapToInt(TransferListener::getExitStatus)
      .sum();

    if (exitStatus != 0) {
      String msg = "Error ! (" + exitStatus + ") errors were seen.";
      DbLoggers.LOGGER_DB_ENGINE.severe(msg);
      //noinspection resource
      this.getTabular().setExitStatus(Math.toIntExact(exitStatus));
    }

    switch (getTransferOutput()) {
      case TARGETS:
        return sourceTargets.stream()
          .map(TransferSourceTarget::getTargetDataPath)
          .distinct()
          .collect(Collectors.toList());
      case INPUTS:
        return sourceTargets.stream()
          .map(TransferSourceTarget::getSourceDataPath)
          .distinct()
          .collect(Collectors.toList());
      default:
        return List.of(Transfers.transfersListenersToDataPath(this.getTabular(), transferListeners));
    }
  }


  /**
   * When executing a pipeline, we may want to listen to collect the listeners
   * for testing or functional purpose
   * This function permits that.
   * Listener stays then local to the execution and no buffer/accumulation problem occurs
   */
  public TransferPipelineStep addConsumerListener(Consumer<TransferListener> listenerConsumer) {
    this.listenerConsumer = listenerConsumer;
    return this;
  }
}
