package com.tabulify.flow.operation;

import com.tabulify.flow.engine.ArgumentEnum;
import net.bytle.type.KeyNormalizer;

public enum TruncatePipelineStepArgument implements ArgumentEnum {

  FORCE("Drop with force (ie drop foreign key)", false, Boolean.class),
  CASCADE("Drop with cascade (ie drop foreign resource)", false, Boolean.class),
  PROCESSING_TYPE("The processing type ((all at once or one by one)", PipelineStepProcessingType.BATCH, PipelineStepProcessingType.class);


  private final Object defaultValue;
  private final String description;
  private final Class<?> clazz;

  TruncatePipelineStepArgument(String description, Object defaultValue, Class<?> clazz) {

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
