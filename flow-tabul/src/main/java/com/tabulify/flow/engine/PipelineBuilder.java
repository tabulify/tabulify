package com.tabulify.flow.engine;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.flow.operation.PipelineStepProcessingType;
import com.tabulify.template.TemplateUriFunction;
import com.tabulify.transfer.TransferManager;
import com.tabulify.transfer.TransferOperation;
import com.tabulify.transfer.TransferPropertiesSystem;
import com.tabulify.transfer.TransferResourceOperations;
import com.tabulify.uri.DataUriBuilder;
import com.tabulify.uri.DataUriNode;
import com.tabulify.uri.DataUriStringNode;
import com.tabulify.exception.CastException;
import com.tabulify.exception.ExceptionWrapper;
import com.tabulify.exception.InternalException;
import com.tabulify.fs.Fs;
import com.tabulify.type.Casts;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.time.DurationShort;

import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PipelineBuilder extends ExecutionNodeBuilder {


  /**
   * The action taken in case of error
   */
  public PipelineOnErrorAction errorAction = (PipelineOnErrorAction) PipelineArgument.ON_ERROR_ACTION.getDefaultValue();
  public TemplateUriFunction parkingSourceTargetFunction;
  public TransferManager parkingTransferManager = TransferManager
    .builder()
    .setTransferPropertiesSystem(TransferPropertiesSystem
      .builder()
      .setSourceOperations(TransferResourceOperations.DROP)
      .setOperation(TransferOperation.INSERT))
    .build();
  /**
   * The pooling interval when no data path is supplied
   * <p>
   * Example:
   * * Local file system operations: 100-500ms (File creation, deletion, or modification checks, Log file monitoring)
   * * Database polling: 1-5 seconds (Checking for new records, Job queue processing, Status updates)
   * * External API polling: 5-30 seconds (REST API status checks, Third-party service integration, Rate-limited services)
   * * Long-running processes: 30 seconds - 5 minutes (Batch job completion, System maintenance tasks, Background data processing)
   */
  private DurationShort pollInterval = (DurationShort) PipelineArgument.POLL_INTERVAL.getDefaultValue();

  /**
   * The wait interval between get in milliseconds
   * <p>
   * If zero, no wait
   * This is used to slow down the stream.
   * For instance, a supplier of generated time is almost instantaneous, you may
   * want to get it only every seconds
   */
  private DurationShort pushInterval = (DurationShort) PipelineArgument.PUSH_INTERVAL.getDefaultValue();

  /**
   * The time in milliseconds of window computation
   * (ie The period in time where the {@link PipelineStepIntermediateManyToManyAbs}
   * step are executed)
   */
  private DurationShort windowInterval = (DurationShort) PipelineArgument.WINDOW_INTERVAL.getDefaultValue();

  /**
   * The maximum number of cycle in case of recurrent or stream processing
   * (one cycle = one data path)
   */
  private long maxCycleCount = (long) PipelineArgument.MAX_CYCLE_COUNT.getDefaultValue();

  /**
   * Do we collect the data path that comes at the end of the stream
   */
  Boolean isDownStreamDataPathCollected = null;
  /**
   * The steps to build
   */
  private final List<PipelineStepBuilder> stepBuilders = new ArrayList<>();
  PipelineCascadeNode pipelineRootNode;

  private boolean isStrict = (Boolean) PipelineArgument.STRICT_EXECUTION.getDefaultValue();

  private DataUriStringNode parkingDataUriString = (DataUriStringNode) PipelineArgument.PARKING_TARGET_URI.getDefaultValue();

  /**
   * Script Variable
   * They are managed and injected
   */
  private Path scriptPath;
  DataUriBuilder dataUriBuilderWithScriptPath;
  private String logicalName;

  /**
   * @param path - the path of the actual running or parsed pipeline script
   * @return the object for chaining
   */
  public PipelineBuilder setScriptPath(Path path) {
    this.scriptPath = path;
    if(!this.scriptPath.isAbsolute()){
      /**
       * Absolute because the parent of a relative path is null if there is only 1 name
       */
      throw new InternalException("script path should be absolute");
    }
    /**
     * Add the {@link SD_LOCAL_FILE_SYSTEM} connection
     */
    DataUriBuilder.DataUriBuilderBuilder dataUriBuilderBuilder = DataUriBuilder.builder(getTabular());
    if (scriptPath != null) {
      dataUriBuilderBuilder.addManifestDirectoryConnection(scriptPath.getParent());
    }
    this.dataUriBuilderWithScriptPath = dataUriBuilderBuilder.build();
    return this;
  }

  public Path getScriptPath() {
    return this.scriptPath;
  }


  public PipelineBuilder setArgument(KeyNormalizer key, Object value) {

    PipelineArgument streamArgument;
    try {
      streamArgument = Casts.cast(key, PipelineArgument.class);
    } catch (CastException e) {
      String expectedKeyAsString = Arrays.stream(PipelineArgument.class.getEnumConstants())
        .sorted()
        .map(c -> KeyNormalizer.createSafe(c).toCliLongOptionName())
        .collect(Collectors.joining(", "));
      throw new IllegalArgumentException("The argument (" + key + ") is unknown for the step " + this + ". We were expecting one of " + expectedKeyAsString);
    }
    Attribute attribute;
    try {
      attribute = this.getTabular().getVault()
        .createVariableBuilderFromAttribute(streamArgument)
        .setOrigin(Origin.PIPELINE)
        .build(value);
      this.setArgument(attribute);
    } catch (CastException e) {
      throw new IllegalArgumentException("The " + streamArgument + " value (" + value + ") of the step (" + this + ") is not conform . Error: " + e.getMessage(), e);
    }

    switch (streamArgument) {
      case POLL_INTERVAL:
        this.pollInterval = (DurationShort) attribute.getValueOrDefault();
        break;
      case PUSH_INTERVAL:
        this.pushInterval = (DurationShort) attribute.getValueOrDefault();
        break;
      case WINDOW_INTERVAL:
        this.windowInterval = (DurationShort) attribute.getValueOrDefault();
        break;
      case TIMEOUT:
        this.timeOut = (DurationShort) attribute.getValueOrDefault();
        break;
      case TIMEOUT_TYPE:
        this.timeoutType = (PipelineTimeoutType) attribute.getValueOrDefault();
        break;
      case ON_ERROR_ACTION:
        this.errorAction = (PipelineOnErrorAction) attribute.getValueOrDefault();
        break;
      case STRICT_EXECUTION:
        this.isStrict = (Boolean) attribute.getValueOrDefault();
        break;
      case PARKING_TARGET_URI:
        this.parkingDataUriString = (DataUriStringNode) attribute.getValueOrDefault();
        break;
      default:
        throw new InternalException("The argument `" + key + "` should be processed for the step (" + this + ")");
    }
    return this;
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
  public PipelineBuilder setPollInterval(DurationShort pollInterval) {
    this.pollInterval = pollInterval;
    return this;
  }

  /**
   * The wait interval between get in milliseconds
   * <p>
   * If zero, no wait
   * This is used to slow down the stream.
   * For instance, a supplier of generated time is almost instantaneous, you may
   * want to get it only every seconds
   */
  public PipelineBuilder setPushInterval(DurationShort pushInterval) {
    this.pushInterval = pushInterval;
    return this;
  }

  /**
   * Timeout
   */
  private DurationShort timeOut = null;
  private PipelineTimeoutType timeoutType;

  /**
   * The time in milliseconds of window computation
   * (ie The period in time where the {@link PipelineStepIntermediateManyToManyAbs}
   * step are executed)
   */
  public PipelineBuilder setWindowInterval(DurationShort windowInterval) {
    this.windowInterval = windowInterval;
    return this;
  }

  /**
   * The maximum number of cycle (ie count of data path) in case of recurrent or stream processing
   */
  public PipelineBuilder setMaxCycleCount(int maxCycleCount) {
    this.maxCycleCount = maxCycleCount;
    return this;
  }

  public DurationShort getPollInterval() {
    return this.pollInterval;
  }

  public DurationShort getPushInterval() {
    return this.pushInterval;
  }

  public DurationShort getWindowInterval() {
    return this.windowInterval;
  }

  @Override
  public PipelineBuilder setTabular(Tabular tabular) {
    return (PipelineBuilder) super.setTabular(tabular);
  }

  public Pipeline build() {



    /**
     * Name identifier
     */
    String logicalName = computeLogicalName();
    if (getNodeName() == null) {
      this.setNodeName(logicalName);
    }


    /**
     * Pipeline and steps
     */
    Pipeline pipeline = new Pipeline(this);
    buildOrRebuildCascadeFromSteps(pipeline);

    /**
     * Derived Meta
     */
    Attribute startTimeAttribute = Attribute.create(PipelineDerivedAttribute.START_TIME, Origin.DEFAULT)
      .setValueProvider(() -> Timestamp.from(pipeline.getPipelineResult().getTotalElapsedTimer().getStartTime()));
    this.setDerivedAttribute(startTimeAttribute);
    Attribute processingTypeAttribute = Attribute.create(PipelineDerivedAttribute.PROCESSING_TYPE, Origin.DEFAULT)
      .setValueProvider(pipeline::getProcessingType);
    this.setDerivedAttribute(processingTypeAttribute);
    Attribute nameAttribute = Attribute.create(PipelineDerivedAttribute.LOGICAL_NAME, Origin.DEFAULT)
      .setValueProvider(pipeline::getNodeName);
    this.setDerivedAttribute(nameAttribute);


    /**
     * The parking target data uri function
     */
    DataUriNode parkingDataUri = this.getDataUri(parkingDataUriString);
    this.parkingSourceTargetFunction = TemplateUriFunction
      .builder(getTabular())
      .setTargetUri(parkingDataUri)
      .setTargetIsContainer(true)
      .setPipeline(pipeline)
      .build();

    /**
     * Last argument
     */
    if (this.isDownStreamDataPathCollected == null) {
      // true by default if batch processing
      this.isDownStreamDataPathCollected = pipelineRootNode.getProcessingType() == PipelineStepProcessingType.BATCH;
    }


    return pipeline;
  }


  private String computeLogicalName() {
    Path path = this.getScriptPath();
    if (path != null) {
      return Fs.getFileNameWithoutExtension(path);
    }
    return "anonymous";
  }


  /**
   * A utility class to build/rebuilt the cascade
   * As a pipeline can be run multiple time, it's an easy way to reset the state
   */
  void buildOrRebuildCascadeFromSteps(Pipeline pipeline) {
    int buildStepCount = 0;
    PipelineCascadeNode actualPipelineNodeBeingBuild = null;
    /**
     * The root of the pipeline
     */
    this.pipelineRootNode = null;
    for (PipelineStepBuilder stepBuilder : this.stepBuilders) {
      /**
       * Context injection
       */
      stepBuilder.setPipeline(pipeline);
      stepBuilder.setTabular(getTabular());
      stepBuilder.setPipelineBuilder(this);

      buildStepCount++;
      stepBuilder.setPipelineStepId(buildStepCount);
      /**
       * For the name
       */
      if (stepBuilder.getNodeName() == null) {
        stepBuilder.setNodeName("step" + (buildStepCount));
      }
      PipelineStep step;
      try {
        step = stepBuilder.build();
      } catch (Exception e) {
        throw ExceptionWrapper.builder(e, "The step " + stepBuilder + " returns an error.")
          .setPosition(ExceptionWrapper.ContextPosition.FIRST)
          .buildAsRuntimeException();
      }

      if (pipelineRootNode == null) {
        if (!PipelineStepRoot.class.isAssignableFrom(step.getClass())) {
          throw new IllegalArgumentException("The first step (" + step.getNodeName() + ", " + step.getClass().getSimpleName() + ") of the pipeline (" + this + ") is not a data resource supplier operation.");
        }
        pipelineRootNode = new PipelineCascadeNode((PipelineStepRoot) step, null);
        actualPipelineNodeBeingBuild = pipelineRootNode;
        continue;
      }

      assert actualPipelineNodeBeingBuild != null;
      if (PipelineStepIntermediateSupplier.class.isAssignableFrom(step.getClass())) {
        actualPipelineNodeBeingBuild = actualPipelineNodeBeingBuild.createChild((PipelineStepIntermediateSupplier) step);
        continue;
      }

      PipelineStepIntermediateMapAbs filterStep;
      try {
        filterStep = (PipelineStepIntermediateMapAbs) step;
      } catch (Exception e) {
        throw new RuntimeException("The step (" + step.getNodeName() + ") of the pipeline (" + this + ") is not a filter map operation but a " + step.getClass().getSimpleName());
      }

      actualPipelineNodeBeingBuild.addMapStep(filterStep);
    }

  }

  public long getMaxCycleCount() {
    return this.maxCycleCount;
  }

  public PipelineBuilder setCollectDownStreamDataPaths(boolean b) {
    this.isDownStreamDataPathCollected = b;
    return this;
  }

  /**
   * @param stepBuilder - take a builder so that we can inject the pipeline
   */
  public PipelineBuilder addStep(PipelineStepBuilder stepBuilder) {

    this.stepBuilders.add(stepBuilder);
    return this;

  }

  @Override
  public PipelineBuilder setNodeName(String nodeName) {
    return (PipelineBuilder) super.setNodeName(nodeName);
  }

  /**
   * @param timeOut     - the timeout value
   * @param timeoutType - the timeout type
   */
  public PipelineBuilder setTimeOut(DurationShort timeOut, PipelineTimeoutType timeoutType) {
    this.timeOut = timeOut;
    this.timeoutType = timeoutType;
    return this;
  }

  public DurationShort getTimeOut() {
    return this.timeOut;
  }

  public PipelineTimeoutType getTimeOutType() {
    return this.timeoutType;
  }


  /**
   * A special data uri builder that takes into account the
   * {@link com.tabulify.connection.ConnectionBuiltIn#MD_LOCAL_FILE_SYSTEM}
   */
  public DataUriNode getDataUri(DataUriStringNode dataUriStringNode) {

    /**
     * If this pipeline comes from a script
     */
    if (this.dataUriBuilderWithScriptPath != null) {
      return this.dataUriBuilderWithScriptPath.apply(dataUriStringNode);
    }

    return this.getTabular().createDataUri(dataUriStringNode);
  }

  public PipelineBuilder setOnErrorAction(PipelineOnErrorAction pipelineOnErrorAction) {
    this.errorAction = pipelineOnErrorAction;
    return this;
  }


  public Boolean isStrict() {
    return isStrict;
  }


}
