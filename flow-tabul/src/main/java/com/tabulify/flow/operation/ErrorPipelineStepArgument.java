package com.tabulify.flow.operation;


import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.type.KeyNormalizer;

public enum ErrorPipelineStepArgument implements ArgumentEnum {


  PROCESSING_TYPE("The processing type", PipelineStepProcessingType.BATCH, PipelineStepProcessingType.class),
  FAIL_EVERY_N_COUNT("Fail every N number of data resources (0 meaning always failing)", 0, Integer.class),
  ENABLE("Enable or disable the step", null, Boolean.class),
  FAIL_POINT("Enable or disable the step", ErrorPipelineStepFailPoint.OUTPUT, ErrorPipelineStepFailPoint.class);

  private final Object defaultValue;
  private final String description;
  private final Class<?> clazz;

  ErrorPipelineStepArgument(String description, Object defaultValue, Class<?> clazz) {

    this.description = description;
    this.defaultValue = defaultValue;
    this.clazz = clazz;

  }

  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.clazz;
  }

  public Object getDefaultValue() {
    return this.defaultValue;
  }

  @Override
  public String toString() {
    return KeyNormalizer.createSafe(name()).toCliLongOptionName();
  }

}
