package net.bytle.db.flow.step;

import net.bytle.db.DbLoggers;
import net.bytle.db.flow.Granularity;
import net.bytle.db.flow.engine.FilterRunnable;
import net.bytle.db.flow.engine.OperationStep;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.SelectException;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.transfer.*;
import net.bytle.exception.CastException;
import net.bytle.exception.NoPathFoundException;
import net.bytle.exception.NoVariableException;
import net.bytle.template.TextTemplate;
import net.bytle.template.TextTemplateEngine;
import net.bytle.type.*;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static net.bytle.db.flow.Granularity.RESOURCE;
import static net.bytle.db.flow.step.TransferArgumentProperty.*;
import static net.bytle.db.flow.step.TransferOutputArgument.TARGETS;

public class TransferStep extends TargetFilterStepAbs {

  public static final String TRANSFER = "transfer";


  public TransferStep() {

    this.getOrCreateArgument(TRANSFER_OPERATION).setValueProvider(() -> this.transferProperties.getOperation().toString());
    this.getOrCreateArgument(TARGET_OPERATION).setValueProvider(() -> this.transferProperties.getTargetOperations());
    this.getOrCreateArgument(TARGET_BATCH_SIZE).setValueProvider(() -> this.transferProperties.getBatchSize());
    this.getOrCreateArgument(TARGET_COMMIT_FREQUENCY).setValueProvider(() -> this.transferProperties.getCommitFrequency());
    this.getOrCreateArgument(TARGET_WORKER_COUNT).setValueProvider(() -> this.transferProperties.getTargetWorkerCount());
    this.getOrCreateArgument(SOURCE_OPERATION).setValueProvider(() -> this.transferProperties.getSourceOperations());
    this.getOrCreateArgument(BUFFER_SIZE).setValueProvider(() -> this.transferProperties.getBufferSize());

  }

  /**
   * Named accepted as operation
   */
  public static final Set<String> acceptedNames;

  static {

    acceptedNames = Arrays.stream(net.bytle.db.transfer.TransferOperation.values())
      .map(e -> Key.toNormalizedKey(e.toString()))
      .collect(Collectors.toSet());
    acceptedNames.add(TRANSFER);

  }


  protected TransferProperties transferProperties = TransferProperties.create();
  private List<TransferListener> allTransfersListeners = new ArrayList<>();

  /**
   * TODO: We should use it to be able to change the target operation (ie no TRUNCATE, REPLACE) if the target was already seen
   */
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final Set<DataPath> targetsAlreadyVisited = new HashSet<>();

  private TransferOutputArgument transferOutput = TARGETS;

  private Granularity granularity = RESOURCE;


  public static TransferStep create() {
    return new TransferStep();
  }

  @Override
  public String getOperationName() {
    return TRANSFER;
  }

  @Override
  public TransferStep setArguments(MapKeyIndependent<Object> arguments) {

    /**
     * Grab the common target argument
     */
    super.setArguments(arguments);

    /**
     * Transfer Properties
     */
    TransferProperties transferProperties = TransferProperties.create();
    this.setTransferProperties(transferProperties);
    for (Map.Entry<String, Object> argument : arguments.entrySet()) {
      Object key = argument.getKey();

      TransferArgumentProperty transferArgumentProperty;
      try {
        transferArgumentProperty = Casts.cast(key.toString(), TransferArgumentProperty.class);
      } catch (CastException e) {
        throw new RuntimeException("The argument (" + key + ") is not a valid argument for the step (" + this + "). You can enter one of the following values: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TransferArgumentProperty.class));
      }
      Object argumentString = argument.getValue();
      switch (transferArgumentProperty) {

        case TRANSFER_OPERATION:
          transferProperties.setOperation(TransferOperation.createFrom(argumentString.toString()));
          break;
        case TRANSFER_MAPPING_METHOD:
          TransferMappingMethod transferMappingMethod;
          try {
            transferMappingMethod = Casts.cast(argumentString.toString(), TransferMappingMethod.class);
          } catch (CastException e) {
            throw new RuntimeException("The value (" + argumentString + ") is not a valid value for the argument " + TRANSFER_MAPPING_METHOD + " on the step (" + this + "). You can enter of the following values: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TransferMappingMethod.class));
          }
          switch (transferMappingMethod) {
            case NAME:
              transferProperties.withColumnMappingByName();
              break;
            case POSITION:
              transferProperties.withColumnMappingByPosition();
              break;
            case MAP:
              // map is given in the other variable
            default:
              throw new RuntimeException("Internal Error: The method (" + transferMappingMethod + ") has not been implemented");
          }
          break;
        case STEP_GRANULARITY:
          try {
            this.setGranularity(Casts.cast(argumentString, Granularity.class));
          } catch (CastException e) {
            throw new RuntimeException("The value (" + argumentString + ") is not a valid value for the argument " + STEP_GRANULARITY + " on the step (" + this + "). You can enter of the following values: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(Granularity.class));
          }
          break;
        case TRANSFER_COLUMN_MAPPING:
          if (!(argumentString instanceof Map)) {
            throw new RuntimeException("The mapping column value (" + argumentString + ") is not a map but a " + argumentString.getClass().getSimpleName());
          }
          try {
            Map<String, String> mapByName = Casts.castToSameMap(argumentString, String.class, String.class);
            transferProperties.withColumnMappingByNamedMap(mapByName);
          } catch (Exception e) {
            try {
              Map<Integer, Integer> mapByPosition = Casts.castToSameMap(argumentString, Integer.class, Integer.class);
              transferProperties.withColumnMappingByPositionalMap(mapByPosition);
            } catch (Exception ex) {
              throw new RuntimeException("The mapping column value should be a map of name or a name of number. This value (" + argumentString + ") is not");
            }
          }
          break;
        case TARGET_OPERATION:
          if (argumentString instanceof String) {
            try {
              transferProperties.addTargetOperations(Casts.cast(argumentString.toString(), TransferResourceOperations.class));
            } catch (CastException e) {
              throw new RuntimeException("The value (" + argumentString + ") is not a valid transfer operation for the argument " + TARGET_OPERATION + " for the step (" + this + "). You can enter one of the following values: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TransferResourceOperations.class));
            }
          } else if (argumentString instanceof List) {
            Collection<Object> values = Casts.castToListSafe(argumentString, Object.class);
            for (Object value : values) {
              try {
                transferProperties.addTargetOperations(Casts.cast(value.toString(), TransferResourceOperations.class));
              } catch (CastException e) {
                throw new RuntimeException("The value (" + value + ") is not a valid transfer operation for the argument " + TARGET_OPERATION + " for the step (" + this + "). You can enter one of the following values: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TransferResourceOperations.class));
              }
            }
          } else {
            throw new IllegalArgumentException("The property `" + TARGET_OPERATION + "` of the step (" + this + ") should be a list or a string but is a " + argumentString.getClass().getSimpleName());
          }
          break;
        case SOURCE_OPERATION:
          if (argumentString instanceof String) {
            try {
              transferProperties.addSourceOperations(Casts.cast(argumentString, TransferResourceOperations.class));
            } catch (CastException e) {
              throw new RuntimeException("The argument (" + argumentString + ") is not a valid transfer operation for the argument " + SOURCE_OPERATION + " for the step (" + this + "). You can enter one of the following values: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TransferResourceOperations.class));
            }
          } else if (argumentString instanceof List) {
            Collection<Object> values = Casts.castToListSafe(argumentString, Object.class);
            for (Object value : values) {
              try {
                transferProperties.addSourceOperations(Casts.cast(value, TransferResourceOperations.class));
              } catch (CastException e) {
                throw new RuntimeException("The argument (" + value + ") is not a valid transfer operation for the argument " + SOURCE_OPERATION + " for the step (" + this + "). You can enter one of the following values: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TransferResourceOperations.class));
              }
            }
          } else {
            throw new IllegalArgumentException("The property `" + SOURCE_OPERATION + "` of the step (" + this + ") should be a list or a string but is a " + argumentString.getClass().getSimpleName());
          }
          break;
        case SOURCE_FETCH_SIZE:
          try {
            transferProperties.setFetchSize(Casts.cast(argumentString, Integer.class));
          } catch (CastException e) {
            throw new RuntimeException("The value (" + argumentString + ") is not a valid integer for the argument " + SOURCE_FETCH_SIZE + " on the step (" + this + ").");
          }
          break;
        case BUFFER_SIZE:
          try {
            transferProperties.setBufferSize(Casts.cast(argumentString, Integer.class));
          } catch (CastException e) {
            throw new RuntimeException("The value (" + argumentString + ") is not a valid integer for the argument " + BUFFER_SIZE + " on the step (" + this + ").");
          }
          break;
        case TARGET_BATCH_SIZE:
          try {
            transferProperties.setBatchSize(Casts.cast(argumentString, Integer.class));
          } catch (CastException e) {
            throw new RuntimeException("The value (" + argumentString + ") is not a valid integer for the argument " + TARGET_BATCH_SIZE + " on the step (" + this + ").");
          }
          break;
        case TARGET_COMMIT_FREQUENCY:
          try {
            transferProperties.setCommitFrequency(Casts.cast(argumentString, Integer.class));
          } catch (CastException e) {
            throw new RuntimeException("The value (" + argumentString + ") is not a valid integer for the argument " + TARGET_COMMIT_FREQUENCY + " on the step (" + this + ").");
          }
          break;
        case TARGET_WORKER_COUNT:
          try {
            transferProperties.setTargetWorkerCount(Casts.cast(argumentString, Integer.class));
          } catch (CastException e) {
            throw new RuntimeException("The value (" + argumentString + ") is not a valid integer for the argument " + TARGET_WORKER_COUNT + " on the step (" + this + ").");
          }
          break;
        case WITH_BIND_VARIABLES:
          try {
            transferProperties.setWithBindVariablesStatement(Casts.cast(argumentString, Boolean.class));
          } catch (CastException e) {
            throw new RuntimeException("The value (" + argumentString + ") is not a valid bo0lean for the argument " + WITH_BIND_VARIABLES + " on the step (" + this + ").");
          }
          break;
        case METRICS_DATA_URI:
          DataPath metricsDataPath = tabular.getDataPath(argumentString.toString());
          transferProperties.setMetricsDataPath(metricsDataPath);
          break;
        case STEP_OUTPUT:
          TransferOutputArgument transferOutput;
          try {
            transferOutput = Casts.cast(argumentString.toString(), TransferOutputArgument.class);
          } catch (CastException e) {
            throw new RuntimeException("The value (" + argumentString + ") is not a valid value for the argument " + STEP_OUTPUT + " for the step (" + this + "). You can enter one of the following values: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TransferOutputArgument.class));
          }
          this.setOutput(transferOutput);
          break;
        default:
          throw new IllegalArgumentException("Internal Error: The transfer property (" + transferArgumentProperty + ") should be taken into account in the arguments");
      }
    }
    return this;
  }

  public TransferStep setOutput(TransferOutputArgument transferOutput) {
    this.transferOutput = transferOutput;
    return this;
  }


  @Override
  public Boolean accept(String name) {

    return acceptedNames.contains(Key.toNormalizedKey(name));

  }

  @Override
  public Set<String> getAcceptedCommandNames() {
    return acceptedNames;
  }

  /**
   * @return if this operation needs to see all resource before starting
   */
  @Override
  public boolean isAccumulator() {
    return true;
  }

  @Override
  public OperationStep setOutput(AttributeValue attribute) {
    this.transferOutput = (TransferOutputArgument) attribute;
    return this;
  }

  @Override
  public AttributeValue getOutput() {
    return this.transferOutput;
  }

  @Override
  public FilterRunnable createRunnable() {
    return new TransferFilterRunnable(this);
  }


  public TransferStep setTransferProperties(TransferProperties transferProperties) {
    this.transferProperties = transferProperties;
    return this;
  }

  public TransferStep addTransferListeners(List<TransferListener> transferListeners) {
    this.allTransfersListeners.addAll(transferListeners);
    return this;
  }

  /**
   * Gives the possibility to the caller to retrieve all transfer listeners
   *
   * @param transferListeners the transfers listeners to listen
   * @return the object for chaining
   */
  public TransferStep injectTransferListenersReference(List<TransferListener> transferListeners) {
    this.allTransfersListeners = transferListeners;
    return this;
  }

  public TransferStep setGranularity(Granularity granularity) {
    this.granularity = granularity;
    return this;
  }


  public class TransferFilterRunnable implements FilterRunnable {

    private final TransferStep transferStep;
    private final Set<DataPath> inputs = new HashSet<>();

    private boolean isDone = false;

    /**
     * The transfer manager (to be able to cancel if needed)
     */
    @SuppressWarnings("FieldCanBeLocal")
    private TransferManager transferManager;
    private Map<DataPath, DataPath> sourceTargets;


    public TransferFilterRunnable(TransferStep transferStep) {
      this.transferStep = transferStep;
    }


    @Override
    public void addInput(Set<DataPath> inputs) {
      this.inputs.addAll(inputs);
    }

    @Override
    public void run() {

      /**
       * Start building the transferManager object
       */
      transferManager = TransferManager.create()
        .setTransferProperties(transferProperties);


      switch (this.transferStep.granularity) {
        case RESOURCE:
          this.sourceTargets = this.getSourceTargetsForResourceRun();
          break;
        case RECORD:
          this.sourceTargets = this.getSourceTargetsForRecordRun();
          break;
        default:
          throw new RuntimeException("Internal Error: the granularity (" + this.transferStep.granularity + ") is not implemented.");
      }

      /**
       * Add the transfers
       */
      for (Map.Entry<? extends DataPath, ? extends DataPath> sourceTarget : sourceTargets.entrySet()) {
        DataPath source = sourceTarget.getKey();
        DataPath target = sourceTarget.getValue();
        TransferSourceTarget transferSourceTarget = TransferSourceTarget.create(
          source,
          target,
          transferProperties
        );
        targetsAlreadyVisited.add(target);
        transferManager.addTransfer(transferSourceTarget);
      }

      /**
       * Start
       */
      List<TransferListener> transferListeners = transferManager
        .run()
        .getTransferListeners();
      this.transferStep.addTransferListeners(transferListeners);


      // Exit
      long exitStatus = transferListeners
        .stream()
        .mapToInt(TransferListener::getExitStatus)
        .sum();

      if (exitStatus != 0) {
        String msg = "Error ! (" + exitStatus + ") errors were seen.";
        DbLoggers.LOGGER_DB_ENGINE.severe(msg);
        TransferStep.this.getTabular().setExitStatus(Math.toIntExact(exitStatus));
      }

      this.isDone = true;

    }

    private Map<DataPath, DataPath> getSourceTargetsForRecordRun() {

      Map<DataPath, DataPath> sourceTargets = new HashMap<>();
      for (DataPath sourceDataPath : inputs) {

        try (SelectStream selectStream = sourceDataPath.getSelectStream()) {

          /**
           * Compile the target uri template
           */
          String targetUriPath;
          try {
            targetUriPath = targetUri.getPath();
          } catch (NoPathFoundException e) {
            throw new IllegalArgumentException("A record run should have a path in the target uri (target uri: " + targetUri + ")");
          }
          TextTemplate targetPathTemplate = TextTemplateEngine.getOrCreate().compile(targetUriPath);
          Map<String, Object> templateVariables = new HashMap<>();

          while (selectStream.next()) {

            /**
             * Compute the target data path logical name
             */
            for (String attributeName : targetPathTemplate.getVariableNames()) {

              Object variableValue;
              try {
                variableValue = sourceDataPath.getVariable(attributeName);
              } catch (NoVariableException e) {
                variableValue = selectStream.getObject(attributeName);
              }

              if (variableValue != null) {
                templateVariables.put(attributeName, variableValue);
              }

            }
            String targetCompiledPath = targetPathTemplate
              .applyVariables(templateVariables)
              .getResult();

            DataPath targetDataPath = targetUri.getConnection()
              .getDataPath(targetCompiledPath);

            DataPath recordSourceDataPath = tabular.getAndCreateRandomMemoryDataPath()
              .getOrCreateRelationDef()
              .copyDataDef(sourceDataPath)
              .getDataPath();
            try (InsertStream insertStream = recordSourceDataPath.getInsertStream()) {
              insertStream.insert(selectStream.getObjects());
            }

            sourceTargets.put(recordSourceDataPath, targetDataPath);

          }
        } catch (SelectException e) {
          throw new RuntimeException("We were unable to get the select stream of " + sourceDataPath + ". Error: " + e.getMessage());
        }
      }
      return sourceTargets;
    }

    private Map<DataPath, DataPath> getSourceTargetsForResourceRun() {

      return SourceTargetHelperFunction
        .create(TransferStep.this.getTabular())
        .setTargetUri(targetUri)
        .setTargetDataDef(targetDataDef)
        .apply(inputs);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      return false;
    }

    @Override
    public boolean isCancelled() {
      return false;
    }

    @Override
    public boolean isDone() {
      return isDone;
    }

    @Override
    public Set<DataPath> get() throws InterruptedException, ExecutionException {
      switch ((TransferOutputArgument) this.transferStep.getOutput()) {
        case TARGETS:
          return new HashSet<>(sourceTargets.values());
        case SOURCES:
          return new HashSet<>(sourceTargets.keySet());
        default:
          return Transfers.transfersListenersToDataPath(tabular, allTransfersListeners);
      }
    }


    @Override
    public Set<DataPath> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return get();
    }

  }
}
