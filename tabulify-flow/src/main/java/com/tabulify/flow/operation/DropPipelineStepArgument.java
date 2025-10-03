package com.tabulify.flow.operation;

import com.tabulify.flow.engine.ArgumentEnum;
import net.bytle.type.KeyNormalizer;

public enum DropPipelineStepArgument implements ArgumentEnum {

  FORCE("Drop with force (ie drop foreign key)", Boolean.class, false),
  CASCADE("Drop with cascade (ie drop foreign resource)", Boolean.class, false),
  PROCESSING_TYPE("Processing type", PipelineStepProcessingType.class, PipelineStepProcessingType.BATCH);

  private final String desc;
  private final Class<?> clazz;
  private final Object valueDef;

  DropPipelineStepArgument(String desc, Class<?> aClass, Object valueDef) {
    this.desc = desc;
    this.clazz = aClass;
    this.valueDef = valueDef;
  }

  @Override
  public String getDescription() {
    return this.desc;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.clazz;
  }

  @Override
  public Object getDefaultValue() {
    return this.valueDef;
  }

  @Override
  public String toString() {
    return KeyNormalizer.createSafe(name()).toCliLongOptionName();
  }
}
