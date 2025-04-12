package com.tabulify.model;

import net.bytle.type.Attribute;

public enum ColumnAttribute implements Attribute {

  POSITION("The position of the column", Integer.class),
  NAME("The name of the column", String.class),
  TYPE( "The type of the column", String.class),
  NULLABLE("If the value may be nullable", Boolean.class),
  COMMENT("The description of the column", String.class),
  PRECISION("The number precision if th type is a number", Integer.class),
  SCALE("The number scale if the type is a number", Integer.class),
  AUTOINCREMENT("True if the column is an auto-increment column", Boolean.class),
  GENERATED("True if the column is generated", Boolean.class);

  private final String desc;
  private final Class<?> clazz;


  ColumnAttribute(String desc, Class<?> clazz) {

    this.clazz = clazz;
    this.desc = desc;

  }


  @Override
  public String toString() {
    return super.toString().toLowerCase();
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
