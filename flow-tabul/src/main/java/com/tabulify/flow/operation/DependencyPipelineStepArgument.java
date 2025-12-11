package com.tabulify.flow.operation;

import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.type.KeyNormalizer;

public enum DependencyPipelineStepArgument implements ArgumentEnum {


  PROCESSING_TYPE("Processing Type", PipelineStepProcessingType.BATCH, PipelineStepProcessingType.class);


  private final Object defaultValue;
  private final String description;
  private final Class<?> clazz;

  DependencyPipelineStepArgument(String description, Object defaultValue, Class<?> clazz) {

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
