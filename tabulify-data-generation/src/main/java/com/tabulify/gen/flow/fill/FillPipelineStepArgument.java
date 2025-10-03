package com.tabulify.gen.flow.fill;

import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.gen.GenDataPathAttribute;
import com.tabulify.uri.DataUriStringNode;
import net.bytle.type.KeyNormalizer;

import java.util.List;

public enum FillPipelineStepArgument implements ArgumentEnum {

  GENERATOR_SELECTOR("A data selectors to select generator resource", DataUriStringNode.class, null),
  GENERATOR_SELECTORS("A list of data selectors to select generator resource", List.class, null),
  MAX_RECORD_COUNT("The default maximum record count", Long.class, GenDataPathAttribute.MAX_RECORD_COUNT.getDefaultValue()),
  ;


  private final String description;
  private final Class<?> clazz;
  private final Object defaultValue;


  FillPipelineStepArgument(String description, Class<?> aClass, Object defaultValue) {

    this.description = description;
    this.clazz = aClass;
    this.defaultValue = defaultValue;

  }


  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.clazz;
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
