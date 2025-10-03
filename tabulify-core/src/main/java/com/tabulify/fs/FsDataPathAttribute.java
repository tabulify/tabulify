package com.tabulify.fs;

import com.tabulify.conf.AttributeEnum;

public enum FsDataPathAttribute implements AttributeEnum {

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
