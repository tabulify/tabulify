package com.tabulify.tpc;

import com.tabulify.connection.ConnectionAttribute;

public enum TpcConnectionAttribute implements ConnectionAttribute {


  SCALE("A property in the datastore that give the scale used. The size of the generated data in Gb (works only for tpc schema)", Double.class, 0.01, true);


  private final String desc;
  private final Class<?> clazz;
  private final Object defaultValue;
  private final boolean isParameter;

  TpcConnectionAttribute(String desc, Class<?> clazz, Object defaultValue, boolean isParameter) {
    this.desc = desc;
    this.clazz = clazz;
    this.defaultValue = defaultValue;
    this.isParameter = isParameter;
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
  public boolean isParameter() {
    return isParameter;
  }

  @Override
  public Object getDefaultValue() {
    return defaultValue;
  }

}
