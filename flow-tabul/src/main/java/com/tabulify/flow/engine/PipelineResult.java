package com.tabulify.flow.engine;

import com.tabulify.conf.Attribute;
import com.tabulify.flow.operation.PipelineStepProcessingType;
import com.tabulify.json.JsonObject;
import com.tabulify.memory.MemoryConnection;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;

import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.time.DurationShort;
import com.tabulify.type.time.Timer;

import java.nio.file.Path;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tabulify.flow.engine.PipelineStepAttribute.OPERATION;

public class PipelineResult {


  private Timer totalElapsedTimer;

  /**
   * Data Path produced that were not consumed
   */
  private final List<DataPath> downStreamDataPaths = new ArrayList<>();
  private final Pipeline pipeline;

  // a counter of data paths that have been received at the end of the pipeline
  private long downPipelineCounter = 0;

  private final Map<PipelineStep, PipelineStepResult> stepResultSet = new HashMap<>();
  /**
   * The total sleep in milliseconds between
   * poll
   */
  private long totalPollWaitTime = 0;
  /**
   * The total sleep in milliseconds between
   * push
   */
  private long totalPushWaitTime;
  /**
   * The timer for the execution
   * (should be equal to the timeout if there is one)
   */
  private Timer executionTimer;

  /**
   * This is the last one
   */
  private DataPath lastParkingDirectory;

  public PipelineResult(Pipeline pipeline) {
    this.pipeline = pipeline;
  }

  /**
   * As a {@link Statement#execute(String) sql statement}, a pipeline may return multiple data path
   *
   * @return
   */
  public List<DataPath> getDownStreamDataPaths() {

    return this.downStreamDataPaths;
  }

  public Pipeline getPipeline() {
    return pipeline;
  }

  public void start() {
    this.totalElapsedTimer = Timer.createFromUuid().start();
  }

  public void stop() {
    this.totalElapsedTimer.stop();
  }

  public JsonObject getJsonResult() {


    JsonObject result = JsonObject.create();


    result.addProperty(LOGICAL_NAME.toJsonCase(), this.pipeline.getNodeName());
    Path path = this.pipeline.getScriptPath();
    if (path != null) {
      result.addProperty(FILE_PATH.toJsonCase(), path.toAbsolutePath());
    }
    result.addProperty(START_TIME.toJsonCase(), totalElapsedTimer.getStartTime())
      .addProperty(END_TIME.toJsonCase(), totalElapsedTimer.getEndTime());


    List<JsonObject> jsonObjects = new ArrayList<>();
    for (PipelineStep operationStep : this.pipeline.getSteps()) {

      JsonObject operationJson = JsonObject.create();
      jsonObjects.add(operationJson);
      operationJson
        .addProperty(KeyNormalizer.createSafe(PipelineStepAttribute.NAME).toJsonCase(), operationStep.getNodeName())
        .addProperty(KeyNormalizer.createSafe(OPERATION).toJsonCase(), operationStep.getOperationName());
      JsonObject args = operationJson.createChildObject("args");
      for (Attribute attribute : operationStep.getArguments()) {
        Object value = attribute.getValueOrDefault();
        String publicName = this.pipeline.getTabular().toPublicName(attribute.getAttributeMetadata().toString());
        if (value != null) {
          args.addProperty(publicName, value);
        } else {
          args.addProperty(publicName, "");
        }
      }
    }
    result.addProperty(KeyNormalizer.createSafe("operations").toJsonCase(), jsonObjects);


    return result;


  }


  /**
   * Names
   */
  static final KeyNormalizer START_TIME = KeyNormalizer.createSafe("StartTime");
  static final KeyNormalizer LOGICAL_NAME = KeyNormalizer.createSafe("logicalName");
  static final KeyNormalizer EXECUTION_ELAPSED_TIME = KeyNormalizer.createSafe("ExecutionElapsedTime");
  static final KeyNormalizer TOTAL_ELAPSED_TIME = KeyNormalizer.createSafe("TotalElapsedTime");
  static final KeyNormalizer PARKING_DATA_URI = KeyNormalizer.createSafe("ParkingDataUri");
  static final KeyNormalizer TOTAL_PUSH_WAIT_TIME = KeyNormalizer.createSafe("TotalPushWaitTime");
  static final KeyNormalizer FILE_PATH = KeyNormalizer.createSafe("filePath");
  static final KeyNormalizer TOTAL_POLL_WAIT_TIME = KeyNormalizer.createSafe("TotalPollWaitTime");
  static final KeyNormalizer TIMEOUT = KeyNormalizer.createSafe("Timeout");
  static final KeyNormalizer END_TIME = KeyNormalizer.createSafe("EndTime");
  static final KeyNormalizer STEP_RESULT_TABLE_NAME = KeyNormalizer.createSafe("step_result");
  static final KeyNormalizer OPERATION_TYPE = KeyNormalizer.createSafe("operation_type");
  static final KeyNormalizer STEP_RESULT_COLUMN_NAME_OPERATION_NAME = KeyNormalizer.createSafe("operation_name");
  static final KeyNormalizer PIPELINE_PATH = KeyNormalizer.createSafe("pipeline-path");
  ;
  static final KeyNormalizer PROCESSING_TYPE = KeyNormalizer.createSafe("processing_type");
  static final KeyNormalizer STEP_ID = KeyNormalizer.createSafe("StepId");
  static final KeyNormalizer STEP_RESULT_COLUMN_NAME_STEP_NAME = KeyNormalizer.createSafe("StepName");
  static final KeyNormalizer STEP_RESULT_COLUMN_NAME_INPUT = KeyNormalizer.createSafe("InputCounter");
  static final KeyNormalizer STEP_RESULT_COLUMN_NAME_OUTPUT = KeyNormalizer.createSafe("OutputCounter");
  static final KeyNormalizer STEP_RESULT_COLUMN_NAME_EXECUTION = KeyNormalizer.createSafe("ExecutionCounter");
  static final KeyNormalizer PARKING_COUNTER = KeyNormalizer.createSafe("ParkingCounter");
  static final KeyNormalizer STEP_RESULT_COLUMN_NAME_ERROR = KeyNormalizer.createSafe("error_counter");

  static final KeyNormalizer PIPELINE_RESULT_TABLE_NAME = KeyNormalizer.createSafe("PipelineResults");
  static final KeyNormalizer METRICS = KeyNormalizer.createSafe("Metrics");
  static final KeyNormalizer VALUE = KeyNormalizer.createSafe("Value");

  public DataPath getPipelineResultsAsDataPath() {

    String comment = "Execution Metrics of the pipeline (" + this.getPipeline().getNodeName() + ")";
    InsertStream insertStream = this.pipeline.getTabular().getMemoryConnection().getAndCreateRandomDataPath()
      .setLogicalName(PIPELINE_RESULT_TABLE_NAME.toSqlCase())
      .setComment(comment)
      .createRelationDef()
      .addColumn(METRICS.toSqlCase())
      .addColumn(VALUE.toSqlCase())
      .getDataPath()
      .getInsertStream();

    Path scriptPath = this.getPipeline().getScriptPath();
    if (scriptPath != null) {
      insertStream.insert(
        PIPELINE_PATH.toKebabCase(),
        scriptPath.toString()
      );
    }
    insertStream.insert(
      PROCESSING_TYPE.toKebabCase(),
      this.getPipeline().getProcessingType()
    );
    insertStream.insert(
      START_TIME.toKebabCase(),
      this.totalElapsedTimer.getStartTime()
    );
    insertStream.insert(
      END_TIME.toKebabCase(),
      this.totalElapsedTimer.getEndTime()
    );
    DurationShort timeout = this.getPipeline().getTimeout();
    if (timeout != null) {
      insertStream.insert(
        TIMEOUT.toKebabCase(),
        timeout.toIsoDuration()
      );
    }
    insertStream.insert(
      TOTAL_ELAPSED_TIME.toKebabCase(),
      DurationShort.create(this.totalElapsedTimer.getDuration()).toIsoDuration()
    );
    insertStream.insert(
      EXECUTION_ELAPSED_TIME.toKebabCase(),
      DurationShort.create(this.executionTimer.getDuration()).toIsoDuration()
    );


    if (this.getPipeline().getProcessingType() == PipelineStepProcessingType.STREAM) {
      insertStream.insert(
        TOTAL_POLL_WAIT_TIME.toKebabCase(),
        DurationShort.create(this.totalPollWaitTime, TimeUnit.MILLISECONDS).toIsoDuration());
      insertStream.insert(
        TOTAL_PUSH_WAIT_TIME.toKebabCase(),
        DurationShort.create(this.totalPushWaitTime, TimeUnit.MILLISECONDS).toIsoDuration());
    }

    int parkingCount = this.getStepResults()
      .stream()
      .mapToInt(PipelineStepResult::getParkingCounter)
      .sum();
    if (parkingCount != 0) {
      insertStream.insert(
        PARKING_DATA_URI.toKebabCase(),
        this.lastParkingDirectory
      );
    }

    insertStream.close();
    return insertStream.getDataPath();
  }

  public DataPath getStepResultsAsDataPath() {

    MemoryConnection memoryConnection = this.pipeline.getTabular().getMemoryConnection();
    String comment = "Step execution results for the pipeline (" + this.getPipeline().getNodeName() + ")";
    InsertStream insertStream = memoryConnection.getAndCreateRandomDataPath()
      .setLogicalName(STEP_RESULT_TABLE_NAME.toSqlCase())
      .setComment(comment)
      .createRelationDef()
      .addColumn(STEP_ID.toSqlCase(), Integer.class)
      .addColumn(STEP_RESULT_COLUMN_NAME_STEP_NAME.toSqlCase())
      .addColumn(STEP_RESULT_COLUMN_NAME_OPERATION_NAME.toSqlCase())
      .addColumn(OPERATION_TYPE.toSqlCase())
      .addColumn(PROCESSING_TYPE.toSqlCase())
      .addColumn(STEP_RESULT_COLUMN_NAME_INPUT.toSqlCase(), Integer.class)
      .addColumn(STEP_RESULT_COLUMN_NAME_OUTPUT.toSqlCase(), Integer.class)
      .addColumn(STEP_RESULT_COLUMN_NAME_OUTPUT.toSqlCase(), Integer.class)
      .addColumn(STEP_RESULT_COLUMN_NAME_EXECUTION.toSqlCase(), Integer.class)
      .addColumn(STEP_RESULT_COLUMN_NAME_ERROR.toSqlCase(), Integer.class)
      .addColumn(PARKING_COUNTER.toSqlCase(), Integer.class)
      .getDataPath()
      .getInsertStream();


    if (!this.stepResultSet.isEmpty()) {


      for (PipelineStepResult stepResult : this.stepResultSet
        .values()
        .stream()
        .sorted()
        .collect(Collectors.toList())
      ) {

        PipelineStep step = stepResult.getStep();
        Integer executionCounter = stepResult.getExecutionCounter();
        insertStream.insert(
          step.getPipelineStepId(),
          step.getNodeName(),
          step.getOperationName(),
          step.getNodeType(),
          step.getProcessingType(),
          stepResult.getInputCounter(),
          stepResult.getOutputCounter(),
          executionCounter,
          stepResult.getErrorCounter(),
          stepResult.getParkingCounter()
        );

      }

    }
    insertStream.close();
    return insertStream.getDataPath();
  }

  public void incrementDownStreamCounter() {
    downPipelineCounter++;
  }

  public long getDownStreamCounter() {
    return this.downPipelineCounter;
  }

  public void addStepResult(PipelineStep step, DataPath dataPath, PipelineStepResultDirection direction) {
    if (this.totalElapsedTimer.hasStopped()) {
      throw new IllegalStateException("This result is final as the pipeline execution has stopped. You can't collect a step result");
    }
    PipelineStepResult stepResult = this.stepResultSet.computeIfAbsent(step, (p) -> PipelineStepResult.builder(step).build());
    stepResult.record(dataPath, direction);
  }

  /**
   * @return the results sorted
   */
  public List<PipelineStepResult> getResults() {
    return this.stepResultSet.values()
      .stream()
      .sorted()
      .collect(Collectors.toList());
  }

  public void addDownStreamDataPath(DataPath pipelineDataPath) {
    if (this.totalElapsedTimer.hasStopped()) {
      throw new IllegalStateException("This result is final as the pipeline execution has stopped. You can't collect a downstream data path");
    }
    // null can happen on filter operations
    if (pipelineDataPath == null) {
      return;
    }
    this.incrementDownStreamCounter();
    if (this.pipeline.getIsDownStreamDataPathCollected()) {
      this.downStreamDataPaths.add(pipelineDataPath);
    }
  }

  /**
   * An execution is:
   * * a poll
   * * a collector scheduler
   */
  public void addStepExecution(PipelineStep step) {
    if (this.totalElapsedTimer.hasStopped()) {
      throw new IllegalStateException("This result is final as the pipeline execution has stopped. You can't collect a step execution");
    }
    PipelineStepResult stepResult = this.stepResultSet.computeIfAbsent(step, (p) -> PipelineStepResult.builder(step).build());
    stepResult.incrementExecutionCounter();
  }

  public Timer getTotalElapsedTimer() {
    return this.totalElapsedTimer;
  }

  public List<PipelineStepResult> getStepResults() {
    return this.stepResultSet
      .values()
      .stream()
      .sorted()
      .collect(Collectors.toList());
  }

  public void addPollSleepTime(long sleepTime) {
    this.totalPollWaitTime += sleepTime;
  }

  public void addPushSleepTime(long sleepTime) {
    this.totalPushWaitTime += sleepTime;
  }

  public void endExecutionTimer() {
    this.executionTimer.stop();
  }

  public void startExecutionTimer() {
    this.executionTimer = Timer.createFromUuid().start();
  }

  public Timer getExecutionElapsedTimer() {
    return this.executionTimer;
  }

  public Duration getTotalPoolWaitTime() {
    return Duration.ofMillis(this.totalPollWaitTime);
  }

  public Duration getTotalPushWaitTime() {
    return Duration.ofMillis(this.totalPushWaitTime);
  }

  public List<DataPath> getAllResultsAsDataPath() {
    return List.of(this.getPipelineResultsAsDataPath(), this.getStepResultsAsDataPath());
  }

  public void addError(PipelineStep step) {
    PipelineStepResult stepResult = this.stepResultSet.computeIfAbsent(step, (p) -> PipelineStepResult.builder(step).build());
    stepResult.incrementErrorCounter();
  }

  public void addParking(PipelineStep step, DataPath targetDataPath) {
    PipelineStepResult stepResult = this.stepResultSet.computeIfAbsent(step, (p) -> PipelineStepResult.builder(step).build());
    stepResult.incrementParkingCounter();
    lastParkingDirectory = targetDataPath;

  }

  public DataPath getLastParkingDirectory() {
    return this.lastParkingDirectory;
  }


}
