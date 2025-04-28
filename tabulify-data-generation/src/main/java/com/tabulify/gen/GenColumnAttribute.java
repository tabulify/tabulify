package com.tabulify.gen;

import net.bytle.type.Attribute;

import java.util.Map;

public enum GenColumnAttribute implements Attribute {

  HIDDEN("Hidden", Boolean.class, false),
  /**
   * The property key giving the data generator data
   */
  DATA_GENERATOR("Data Generation Properties", Map.class, null);

  private final String desc;
  private final Class<?> aClass;
  private final Object defaultValue;

  GenColumnAttribute(String hidden, Class<?> booleanClass, Object defaultValue) {
    this.desc = hidden;
    this.aClass = booleanClass;
    this.defaultValue = defaultValue;
  }

  @Override
  public String getDescription() {
    return this.desc;
  }

  @Override
  public Object getDefaultValue() {
    return this.defaultValue;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.aClass;
  }

}
