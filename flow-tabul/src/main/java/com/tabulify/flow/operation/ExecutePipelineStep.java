package com.tabulify.flow.operation;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.flow.engine.PipelineStep;
import com.tabulify.flow.engine.PipelineStepBuilderTarget;
import com.tabulify.fs.binary.FsBinaryDataPath;
import com.tabulify.memory.MemoryDataPath;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.StrictException;
import com.tabulify.spi.TabularType;
import com.tabulify.spi.Tabulars;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.template.TemplateMetas;
import com.tabulify.template.TemplatePrefix;
import com.tabulify.template.TemplateUriFunction;
import com.tabulify.type.time.Timer;
import com.tabulify.uri.DataUriNode;
import com.tabulify.uri.DataUriStringNode;
import com.tabulify.exception.*;
import com.tabulify.type.Casts;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.Lists;

import java.util.List;
import java.util.stream.Collectors;

import static com.tabulify.flow.operation.DropPipelineStepArgument.PROCESSING_TYPE;
import static com.tabulify.flow.operation.ExecutePipelineStepArgument.Constants.DEFAULT_RESULTS_COLUMNS_WITH_COUNT;
import static com.tabulify.flow.operation.ExecutePipelineStepArgument.Constants.DEFAULT_RESULTS_COLUMNS_WITH_LOOP;
import static com.tabulify.flow.operation.ExecutionMode.LOAD;
import static com.tabulify.flow.operation.ExecutionMode.TRANSFER;

public class ExecutePipelineStep extends PipelineStepBuilderTarget {

  static final KeyNormalizer EXEC = KeyNormalizer.createSafe("exec");


  private PipelineStepProcessingType processingType = (PipelineStepProcessingType) PROCESSING_TYPE.getDefaultValue();
  protected boolean strictInput = (Boolean) ExecutePipelineStepArgument.STRICT_INPUT.getDefaultValue();

  StepOutputArgument output = (StepOutputArgument) ExecutePipelineStepArgument.OUTPUT_TYPE.getDefaultValue();
  Boolean strictExecution = (Boolean) ExecutePipelineStepArgument.STRICT_EXECUTION.getDefaultValue();
  private ExecutionMode executionMode = (ExecutionMode) ExecutePipelineStepArgument.EXECUTION_MODE.getDefaultValue();
  private Boolean runtimeResultPersistence = (Boolean) ExecutePipelineStepArgument.RUNTIME_RESULT_PERSISTENCE.getDefaultValue();
  private Boolean stopEarly = (Boolean) ExecutePipelineStepArgument.STOP_EARLY.getDefaultValue();

  private List<ExecuteResultAttribute> resultsColumns = Casts.castToNewListSafe(ExecutePipelineStepArgument.OUTPUT_RESULT_COLUMNS.getDefaultValue(), ExecuteResultAttribute.class);
  private DataUriStringNode errDataUriStringNode = (DataUriStringNode) ExecutePipelineStepArgument.ERROR_DATA_URI.getDefaultValue();
  private boolean failOnError = (boolean) ExecutePipelineStepArgument.FAIL_ON_ERROR.getDefaultValue();
  ;
  private TemplateUriFunction stdErrTemplateUriFunction;
  /**
   * A pointer to trace if error were seen
   */
  private boolean errorWereSeen = false;


  public static ExecutePipelineStep builder() {

    return new ExecutePipelineStep();

  }

  @Override
  public ExecutePipelineStep createStepBuilder() {
    return new ExecutePipelineStep();
  }

  @Override
  public List<Class<? extends ArgumentEnum>> getArgumentEnums() {
    return List.of(DropPipelineStepArgument.class);
  }

  /**
   * @param executionMode - if the results columns are not specified, the default depend on it
   * @return the results column
   */
  List<ExecuteResultAttribute> getResultColumnsByExecutionMode(ExecutionMode executionMode) {
    if (this.resultsColumns != null && !this.resultsColumns.isEmpty()) {
      return this.resultsColumns;
    }
    ExecutionMode localExecutionMode = this.executionMode;
    if (localExecutionMode == null) {
      localExecutionMode = executionMode;
    }
    switch (localExecutionMode) {
      case LOAD:
        return DEFAULT_RESULTS_COLUMNS_WITH_COUNT;
      case TRANSFER:
        return DEFAULT_RESULTS_COLUMNS_WITH_LOOP;
      default:
        throw new MissingSwitchBranch("executionMode", executionMode);
    }
  }

  @Override
  public PipelineStep build() {

    if (this.getTargetUri() == null) {
      DataUriNode resultsDataUri = this.getPipelineBuilder().getDataUri(ExecutePipelineStepArgument.Constants.DEFAULT_TARGET_DATA_URI);
      this.setTargetDataUri(resultsDataUri);
    }

    DataUriNode stdErrUriNode = this.getPipelineBuilder().getDataUri(this.errDataUriStringNode);
    this.stdErrTemplateUriFunction = TemplateUriFunction.builder(getTabular())
      .setTargetUri(stdErrUriNode)
      .setStrict(this.strictExecution)
      .build();


    if (processingType == PipelineStepProcessingType.STREAM) {
      return new ExecutePipelineStream(this);
    }
    return new ExecutePipelineBatch(this);
  }


  public ExecutePipelineStep setProcessingType(PipelineStepProcessingType processingType) {
    this.processingType = processingType;
    return this;
  }

  @Override
  public KeyNormalizer getOperationName() {
    return EXEC;
  }

  public DataPath createResultDataPath(String hash, ExecutionMode executionMode) {
    String logicalName = "execution_results";
    MemoryDataPath dataPath = (MemoryDataPath) this.getTabular()
      .getMemoryConnection()
      .getDataPath(logicalName + "_" + hash)
      .setLogicalName(logicalName)
      .setComment("List of runtime executed");
    RelationDef relationDef = dataPath.getOrCreateRelationDef();
    for (ExecuteResultAttribute executeResultAttribute : this.getResultColumnsByExecutionMode(executionMode)) {
      ColumnDef<?> column = relationDef.getOrCreateColumn(
          executeResultAttribute.toKeyNormalizer().toSqlCase(),
          executeResultAttribute.getValueClazz()
        )
        .setComment(executeResultAttribute.getDesc());
      Integer precision = executeResultAttribute.getPrecision();
      if (precision != null) {
        column.setPrecision(precision);
      }
    }
    return dataPath;

  }


  public ExecutePipelineStep setArgument(KeyNormalizer key, Object value) {

    ExecutePipelineStepArgument executePipelineStepArgument;
    try {
      executePipelineStepArgument = Casts.cast(key, ExecutePipelineStepArgument.class);
    } catch (CastException e) {
      super.setArgument(key, value);
      return this;
    }
    Attribute attribute;
    try {
      attribute = this.getPipeline().getTabular().getVault()
        .createVariableBuilderFromAttribute(executePipelineStepArgument)
        .setOrigin(Origin.DEFAULT)
        .build(value);
      this.setArgument(attribute);
    } catch (CastException e) {
      throw new IllegalArgumentException("The " + key.toCliLongOptionName() + " value (" + value + ") of the step (" + this + ") is not conform . Error: " + e.getMessage(), e);
    }
    Object valueOrDefault = attribute.getValueOrDefault();
    switch (executePipelineStepArgument) {
      case STRICT_INPUT:
        this.setStrictInput(attribute.getValueOrDefaultCastAsSafe(Boolean.class));
        break;
      case STRICT_EXECUTION:
        this.setStrictExecution(attribute.getValueOrDefaultCastAsSafe(Boolean.class));
        break;
      case FAIL_ON_ERROR:
        this.setFailOnError(attribute.getValueOrDefaultCastAsSafe(Boolean.class));
        break;
      case OUTPUT_TYPE:
        try {
          this.setOutputType(Casts.cast(valueOrDefault, StepOutputArgument.class));
        } catch (CastException e) {
          throw IllegalArgumentExceptions.createFromMessageWithPossibleValues(
            "The value (" + valueOrDefault + ") is not a valid output type.",
            StepOutputArgument.class,
            e);
        }
        break;
      case PROCESSING_TYPE:
        try {
          this.setProcessingType(Casts.cast(valueOrDefault, PipelineStepProcessingType.class));
        } catch (CastException e) {
          throw IllegalArgumentExceptions.createFromMessageWithPossibleValues(
            "The value (" + valueOrDefault + ") is not a valid processing type.",
            PipelineStepProcessingType.class,
            e);
        }
        break;
      case EXECUTION_MODE:
        try {
          this.setExecutionMode(Casts.cast(valueOrDefault, ExecutionMode.class));
        } catch (CastException e) {
          throw IllegalArgumentExceptions.createFromMessageWithPossibleValues(
            "The value (" + valueOrDefault + ") is not a valid execution type.",
            ExecutionMode.class,
            e);
        }
        break;
      case STOP_EARLY:
        this.setStopEarly(attribute.getValueOrDefaultCastAsSafe(Boolean.class));
        break;
      case RUNTIME_RESULT_PERSISTENCE:
        this.setRuntimeResultPersistence(attribute.getValueOrDefaultCastAsSafe(Boolean.class));
        break;
      case ERROR_DATA_URI:
        this.setErrDataUri((DataUriStringNode) attribute.getValueOrDefault());
        break;
      case OUTPUT_RESULT_COLUMNS:
        try {
          this.setResultsColumns(Casts.castToNewList(value, ExecuteResultAttribute.class));
        } catch (CastException e) {
          throw IllegalArgumentExceptions.createFromMessageWithPossibleValues(
            "The results value (" + value + ") is not conform . Error: " + e.getMessage(),
            ExecuteResultAttribute.class,
            e);
        }
      default:
        throw new InternalException("The execute argument (" + executePipelineStepArgument + ") was forgotten in the switch branch");
    }
    return this;
  }

  public ExecutePipelineStep setResultsColumns(List<ExecuteResultAttribute> executeResultAttributes) {
    // making them distinct
    this.resultsColumns = executeResultAttributes.stream().distinct().collect(Collectors.toList());
    return this;
  }

  public ExecutePipelineStep setErrDataUri(DataUriStringNode dataUri) {
    this.errDataUriStringNode = dataUri;
    return this;
  }


  public ExecutePipelineStep setStopEarly(Boolean stopEarly) {
    this.stopEarly = stopEarly;
    return this;
  }


  public ExecutePipelineStep setStrictExecution(Boolean aBoolean) {
    this.strictExecution = aBoolean;
    return this;
  }

  public ExecutePipelineStep setStrictInput(boolean strictInput) {
    this.strictInput = strictInput;
    return this;
  }


  public DataPath execute(List<DataPath> runtimeDataPaths) {

    // name of the data path should be unique
    String hash = Lists.toHash(runtimeDataPaths.stream()
      .map(DataPath::toString)
      .collect(Collectors.toList())
    );
    ExecutionMode localExecutionMode = this.getExecutionMode(runtimeDataPaths);
    DataPath dataPath = createResultDataPath(hash, localExecutionMode);


    try (InsertStream insertStream = dataPath.getInsertStream()) {
      for (DataPath runtime : runtimeDataPaths) {

        ExecutionResult result;
        /**
         * Stop early on stream processing
         */
        if (this.stopEarly && this.errorWereSeen) {

          result = ExecutionResult.build()
            .setRuntime(runtime)
            .setCount(0L)
            .setError(new RuntimeException("Stopped early"))
            .build();

        } else {

          if (!Tabulars.isRuntime(runtime)) {
            if (this.strictInput) {
              String msg = "The input data path (" + runtime + ") is not a runtime";
              throw new StrictException(msg);
            }
            runtime = runtime.getConnection().getRuntimeDataPath(runtime, null);
          }

          result = this.apply(runtime, localExecutionMode);
        }

        List<?> record = result.getRecord(this.getResultColumnsByExecutionMode(localExecutionMode));
        insertStream.insert(record);

        /**
         *
         */
        if (result.getError() != null) {
          this.errorWereSeen = true;
        }
        /**
         * Throw error for stack trace
         * Ultimately, the stack trace needs to go in an error stream
         */
        if (this.shouldWeThrow(result)) {
          throw ExceptionWrapper.builder(result.getError(), "Unexpected error")
            .setPosition(ExceptionWrapper.ContextPosition.FIRST)
            .buildAsRuntimeException();
        }
        /**
         * We stop in the list
         */
        if (this.stopEarly && result.getError() != null) {
          break;
        }
      }
    }
    return dataPath;

  }

  /**
   * If not set, execution  mode is determined dynamically based on the input
   *
   * @return the execution mode set or the derived one
   */
  private ExecutionMode getExecutionMode(List<DataPath> runtimeInputDataPaths) {
    if (this.executionMode != null) {
      return this.executionMode;
    }

    for (DataPath runtime : runtimeInputDataPaths) {
      /**
       * If any LOG tabular type, transfer
       */
      if (runtime.getTabularType() == TabularType.COMMAND) {
        return TRANSFER;
      }
    }
    return LOAD;
  }

  private boolean shouldWeThrow(ExecutionResult result) {
    Exception error = result.getError();
    if (error == null) {
      return false;
    }
    // dev error are error in dev that we throw in all cases in dev
    boolean devError = getTabular().isIdeEnv() && error.getClass().equals(NullPointerException.class);
    if (devError) {
      return true;
    }
    return this.strictExecution;


  }

  public ExecutePipelineStep setOutputType(StepOutputArgument stepOutputArgument) {
    this.output = stepOutputArgument;
    return this;
  }

  /**
   * Execute one item and return a result
   */
  private ExecutionResult apply(DataPath runtimeDataPath, ExecutionMode executionMode) {
    Timer timer = Timer.create("execute").start();
    Long recordCount = 0L;


    ExecutionResult.ExecutionResultBuilder executionResult = ExecutionResult.build()
      .setRuntime(runtimeDataPath);
    executionResult.setExecutionType(executionMode);
    try {
      switch (executionMode) {
        case LOAD:
          recordCount = runtimeDataPath.getCount();
          break;
        case TRANSFER:
          /**
           * We can't get a select stream on a runtime
           * May be use {@link com.tabulify.transfer.TransferManager}
           */
          DataPath results = runtimeDataPath.execute();
          try (
            SelectStream selectStream = results.getSelectStream();
            InsertStream insertStream = getResultInsertStream(results, executionMode)
          ) {
            if (insertStream != null) {
              executionResult.setResult(insertStream.getDataPath());
            }
            while (selectStream.next()) {
              recordCount++;
              if (insertStream != null) {
                insertStream.insert(selectStream.getObjects());
              }
            }
          }
          break;
        default:
          throw new MissingSwitchBranch("execution mode", executionMode);
      }
    } catch (Exception e) {
      TemplateMetas templateBuilder = TemplateMetas.builder()
        .addMeta(getPipeline(), TemplatePrefix.PIPELINE)
        .addInputDataPath(runtimeDataPath);
      DataPath errorDataPath = this.stdErrTemplateUriFunction.apply(runtimeDataPath, templateBuilder);
      Tabulars.createIfNotExist(errorDataPath);
      try (InsertStream insertStream = errorDataPath.getInsertStream()) {
        insertStream.insert("Error Message:");
        insertStream.insert(e.getMessage());
        insertStream.insert("Error Stack:");
        for (StackTraceElement element : e.getStackTrace()) {
          insertStream.insert(element.toString());
        }
        DataPath executable = runtimeDataPath.getExecutableDataPath();
        insertStream.insert("Runtime Executable: " + executable);
        insertStream.insert("Runtime Connection: " + runtimeDataPath.getConnection());
        if (!(executable instanceof FsBinaryDataPath)) {
          insertStream.insert("Original Executable Content: ");
          try (SelectStream executableStream = executable.getSelectStreamSafe()) {
            while (executableStream.next()) {
              insertStream.insert(executableStream.getObjects());
            }
          }
        }
      }
      if (this.failOnError) {
        this.getPipeline().setExitStatus(1);
      }
      executionResult
        .setError(e)
        .setErrorDataPath(errorDataPath);
    }
    return executionResult
      .setCount(recordCount)
      .setTimer(timer)
      .build();

  }

  private InsertStream getResultInsertStream(DataPath runtimeDataPath, ExecutionMode localExecutionMode) {
    Boolean storeRequestResult = this.runtimeResultPersistence;
    if (storeRequestResult == null) {
      switch (localExecutionMode) {
        case LOAD:
          storeRequestResult = false;
          break;
        case TRANSFER:
          storeRequestResult = true;
          break;
        default:
          throw new MissingSwitchBranch("executionType", localExecutionMode);
      }
    } else {
      if (localExecutionMode == LOAD && storeRequestResult) {
        throw new IllegalStateException("With a count execution type, store result should be false (" + runtimeDataPath + ")");
      }
    }
    if (!storeRequestResult) {
      return null;
    }
    TemplateMetas templateMetas = TemplateMetas.builder()
      .addMeta(this.getPipeline(), TemplatePrefix.PIPELINE)
      .addInputDataPath(runtimeDataPath);
    DataPath result = this.getTargetUriFunction().apply(runtimeDataPath, templateMetas);
    // What fucked up is fucked up
    if (Tabulars.exists(result)) {
      Tabulars.drop(result);
      // Target uri function on csv read and create metadata
      // pffff
      result = getTabular().getDataPath(result.toDataUri(), null);
    }
    result.mergeDataDefinitionFrom(runtimeDataPath);
    Tabulars.create(result);
    return result.getInsertStream();

  }


  /**
   * Execution
   *
   * @param runtimeDataPath - the data path to execute
   * @return data
   */
  public DataPath execute(DataPath runtimeDataPath) {

    return execute(List.of(runtimeDataPath));

  }

  public ExecutePipelineStep setExecutionMode(ExecutionMode executionMode) {
    this.executionMode = executionMode;
    return this;
  }

  public ExecutePipelineStep setRuntimeResultPersistence(Boolean runtimeResultPersistence) {
    this.runtimeResultPersistence = runtimeResultPersistence;
    return this;
  }

  public ExecutePipelineStep setFailOnError(boolean b) {
    this.failOnError = b;
    return this;
  }
}
