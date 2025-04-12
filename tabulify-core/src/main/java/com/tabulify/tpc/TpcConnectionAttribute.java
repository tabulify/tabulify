package com.tabulify.tpc;

import net.bytle.type.Attribute;

public enum TpcConnectionAttribute implements Attribute {


  SCALE("A property in the datastore that give the scale used. The size of the generated data in Gb (works only for tpc schema)", Double.class);


  private final String desc;
  private final Class<?> clazz;

  TpcConnectionAttribute( String desc, Class<?> clazz) {
    this.desc = desc;
    this.clazz = clazz;
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
  public Object getDefaultValue() {
    return null;
  }

}
