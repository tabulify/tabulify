package com.tabulify.flow.operation;


import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.type.KeyNormalizer;

import java.util.List;
import java.util.Map;

public enum DefinePipelineStepArgument implements ArgumentEnum {


  DATA_RESOURCES("Multiple inline data resources", List.class, null),
  DATA_RESOURCE("One inline data resources", Map.class, null),
  PROCESSING_TYPE("Processing type", PipelineStepProcessingType.class, PipelineStepProcessingType.BATCH),
  ;



  private final String description;
  private final Class<?> clazz;
  private final Object defaultValue;


  DefinePipelineStepArgument(String description, Class<?> clazz, Object defaultValue) {

    this.description = description;
    this.clazz = clazz;
    this.defaultValue = defaultValue;

  }


  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public Class<?> getValueClazz() {
    return clazz;
  }

  @Override
  public Object getDefaultValue() {
    return this.defaultValue;
  }

  @Override
  public String toString() {
    return KeyNormalizer.createSafe(name()).toCliLongOptionName();
  }

}
