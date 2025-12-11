package com.tabulify.flow.operation;

import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.type.KeyNormalizer;

import java.util.ArrayList;
import java.util.List;

public enum InfoPipelineStepArgument implements ArgumentEnum {

  EXCLUDED_ATTRIBUTES("List of excluded data attributes", List.class, new ArrayList<>());

  private final Class<?> valueClass;
  private final String desc;
  private final Object defaultValue;

  InfoPipelineStepArgument(String desc, Class<?> aClass, Object defaultValue) {
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
