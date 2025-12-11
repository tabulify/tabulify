package com.tabulify.jdbc;

import com.tabulify.conf.AttributeEnum;

public enum SqlDataPathAttribute implements AttributeEnum {


  CATALOG("The catalog of the sql resource", String.class, null),
  SCHEMA("The schema of the sql resource", String.class, null),
  NAME("The name of the sql resource", String.class, null);

  private final String desc;
  private final Class<?> valueClazz;
  private final Object valueDefault;

  SqlDataPathAttribute(String description, Class<?> valueClazz, Object valueDefault) {
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
