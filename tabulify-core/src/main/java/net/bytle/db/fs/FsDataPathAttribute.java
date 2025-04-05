package net.bytle.db.fs;

import net.bytle.type.Attribute;

public enum FsDataPathAttribute implements Attribute {
  URI("The URI of the file",java.net.URI.class);

  private final String desc;
  private final Class<?> aClass;

  FsDataPathAttribute(String desc, Class<?> aClass) {
    this.desc = desc;
    this.aClass = aClass;
  }

  @Override
  public String getDescription() {
    return desc;
  }

  @Override
  public Class<?> getValueClazz() {
    return aClass;
  }

  @Override
  public Object getDefaultValue() {
    return null;
  }

}
