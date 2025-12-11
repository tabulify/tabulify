package com.tabulify.flow.operation;

import com.tabulify.flow.Granularity;
import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.type.KeyNormalizer;

public enum SplitPipelineStepArgument implements ArgumentEnum {


  TARGET_TEMPLATE("A target template for the name definition (random by default)", null, String.class),
  GRANULARITY("The granularity of the operation", Granularity.RECORD, Granularity.class)
  ;


  private final Object defaultValue;
  private final String description;
  private final Class<?> clazz;

  SplitPipelineStepArgument(String description, Object defaultValue, Class<?> clazz) {

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
