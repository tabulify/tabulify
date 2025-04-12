package com.tabulify.flow.step;

import net.bytle.type.Attribute;

public enum HeadFunctionArgument implements Attribute {
  LIMIT("The limit of records returned", Integer.class,10);

  private final Class<?> valueClass;
  private final String desc;
  private final Object defaultValue;

  HeadFunctionArgument(String desc, Class<?> aClass, Object defaultValue) {
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
}
