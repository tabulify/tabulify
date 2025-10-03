package com.tabulify.jdbc;

import com.tabulify.conf.AttributeEnum;
import net.bytle.type.KeyNormalizer;

public enum SqlParameterAttribute implements AttributeEnum {
  VALUE("The value", Object.class, null),
  DIRECTION("The direction", SqlParameterDirection.class, SqlParameterDirection.IN),
  TYPE("The value data type", KeyNormalizer.class, null),
  NAME("The name", KeyNormalizer.class, null);

  private final String desc;
  private final Class<?> valueClazz;
  private final Object valueDefault;

  SqlParameterAttribute(String description, Class<?> valueClazz, Object valueDefault) {
    this.desc = description;
    this.valueClazz = valueClazz;
    this.valueDefault = valueDefault;
  }

  @Override
  public String getDescription() {
    return this.desc;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.valueClazz;
  }

  @Override
  public Object getDefaultValue() {
    return this.valueDefault;
  }
}
