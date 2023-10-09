package net.bytle.db.flow.step;

import net.bytle.type.Attribute;

public enum DropFunctionArgument implements Attribute {
  WITH_FORCE("Drop with force",Boolean.class,false);

  private final String desc;
  private final Class<?> clazz;
  private final Object valueDef;

  DropFunctionArgument(String desc, Class<?> aClass, Object valueDef) {
    this.desc = desc;
    this.clazz = aClass;
    this.valueDef = valueDef;
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
    return this.valueDef;
  }
}
