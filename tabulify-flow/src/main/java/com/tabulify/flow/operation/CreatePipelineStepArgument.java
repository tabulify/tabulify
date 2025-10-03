package com.tabulify.flow.operation;

import com.tabulify.flow.engine.ArgumentEnum;
import net.bytle.type.KeyNormalizer;

public enum CreatePipelineStepArgument implements ArgumentEnum {


  OUTPUT("Data attributes in a data definition format", CreatePipelineStepOutputArgument.class, CreatePipelineStepOutputArgument.RESULTS),
  PROCESSING_TYPE("Processing type", PipelineStepProcessingType.class, PipelineStepProcessingType.BATCH);


  private final String description;
  private final Class<?> valueClazz;
  private final Object defaultValue;


  CreatePipelineStepArgument(String description, Class<?> valueClazz, Object defaultValue) {

    this.description = description;
    this.valueClazz = valueClazz;
    this.defaultValue = defaultValue;
  }


  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.valueClazz;
  }

  @Override
  public Object getDefaultValue() {
    return defaultValue;
  }

  @Override
  public String toString() {
    return KeyNormalizer.createSafe(name()).toCliLongOptionName();
  }

}

