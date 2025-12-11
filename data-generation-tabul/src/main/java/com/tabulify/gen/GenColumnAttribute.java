package com.tabulify.gen;

import com.tabulify.conf.AttributeEnum;
import com.tabulify.type.KeyNormalizer;

import java.util.Map;

public enum GenColumnAttribute implements AttributeEnum {

  HIDDEN("Hidden", Boolean.class, false),
  /**
   * The property key giving the data generator data
   */
  DATA_SUPPLIER("Data Supplier Properties", Map.class, null);

  private final String desc;
  private final Class<?> aClass;
  private final Object defaultValue;
  private final KeyNormalizer keyNormalizer;

  GenColumnAttribute(String hidden, Class<?> booleanClass, Object defaultValue) {
    this.desc = hidden;
    this.aClass = booleanClass;
    this.defaultValue = defaultValue;
    this.keyNormalizer = KeyNormalizer.createSafe(this.name());
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

  @Override
  public String toString() {
    return KeyNormalizer.createSafe(this.name()).toKebabCase();
  }

  @Override
  public KeyNormalizer toKeyNormalizer() {
    return keyNormalizer;
  }
}
