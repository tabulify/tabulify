package com.tabulify.flow.operation;

import com.tabulify.flow.engine.ArgumentEnum;
import net.bytle.type.KeyNormalizer;

public enum TailPipelineStepArgument implements ArgumentEnum {

  LIMIT("The limit of records returned", Integer.class, 10);

  private final Class<?> valueClass;
  private final String desc;
  private final Object defaultValue;

  TailPipelineStepArgument(String desc, Class<?> aClass, Object defaultValue) {
    this.desc = desc;
    this.valueClass = aClass;
    this.defaultValue = defaultValue;
  }

  @Override
  public String getDescription() {
    return this.desc;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.valueClass;
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
