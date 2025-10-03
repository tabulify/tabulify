package com.tabulify.flow.operation;

import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.uri.DataUriStringNode;
import net.bytle.type.KeyNormalizer;

import java.util.List;


public enum ExecutePipelineStepArgument implements ArgumentEnum {

  EXECUTION_MODE("Defines the execution mode", ExecutionMode.class, null),
  ERROR_DATA_URI("A template data uri that specifies where to store errors information", DataUriStringNode.class, Constants.DEFAULT_ERROR_DATA_URI),
  OUTPUT_TYPE("The resource type that is passed as output", StepOutputArgument.class, StepOutputArgument.RESULTS),
  OUTPUT_RESULT_COLUMNS("The returned columns of the execution results", List.class, null),
  PROCESSING_TYPE("Processing type", PipelineStepProcessingType.class, PipelineStepProcessingType.BATCH),
  RUNTIME_RESULT_PERSISTENCE("Should we keep the results of a runtime request", Boolean.class, true),
  STOP_EARLY("If true, the execution will stop at the first error", Boolean.class, false),
  STRICT_INPUT("If true, if an input is not a runtime resource, an error is thrown", Boolean.class, false),
  STRICT_EXECUTION("If true, the execution will throw if any error occurs", Boolean.class, false),
  TARGET_DATA_URI("A template data uri that specifies where to store the execution results", DataUriStringNode.class, Constants.DEFAULT_TARGET_DATA_URI),
  ;


  private final String desc;
  private final Class<?> aClass;
  private final Object defaultValue;

  ExecutePipelineStepArgument(String desc, Class<?> aClass, Object defaultValue) {
    this.desc = desc;
    this.aClass = aClass;
    this.defaultValue = defaultValue;
  }

  @Override
  public String getDescription() {
    return desc;
  }

  @Override
  public Object getDefaultValue() {
    return this.defaultValue;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.aClass;
  }

  @Override
  public String toString() {
    return KeyNormalizer.createSafe(name()).toCliLongOptionName();
  }

  public static class Constants {

    public static final DataUriStringNode DEFAULT_ERROR_DATA_URI =
      DataUriStringNode.createFromStringSafe("execute/${pipeline_start_time}-pipe-${pipeline_logical_name}/${input_logical_name}-err.log@tmp");

    public static final DataUriStringNode DEFAULT_TARGET_DATA_URI =
      DataUriStringNode.createFromStringSafe("execute/${pipeline_start_time}-pipe-${pipeline_logical_name}/${input_logical_name}.log@tmp");

    public static final List<ExecuteResultAttribute> DEFAULT_RESULTS_COLUMNS_WITH_LOOP = List.of(
      ExecuteResultAttribute.RUNTIME_DATA_URI,
      ExecuteResultAttribute.EXIT_CODE,
      ExecuteResultAttribute.COUNT,
      ExecuteResultAttribute.LATENCY,
      ExecuteResultAttribute.RESULT_DATA_URI,
      ExecuteResultAttribute.ERROR_MESSAGE
    );
    public static final List<ExecuteResultAttribute> DEFAULT_RESULTS_COLUMNS_WITH_COUNT = List.of(
      ExecuteResultAttribute.RUNTIME_DATA_URI,
      ExecuteResultAttribute.EXIT_CODE,
      ExecuteResultAttribute.COUNT,
      ExecuteResultAttribute.LATENCY,
      ExecuteResultAttribute.ERROR_MESSAGE
    );
  }


}
