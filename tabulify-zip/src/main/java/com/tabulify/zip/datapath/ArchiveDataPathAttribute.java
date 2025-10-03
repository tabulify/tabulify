package com.tabulify.zip.datapath;

import com.tabulify.conf.AttributeEnum;

public enum ArchiveDataPathAttribute implements AttributeEnum {


  ENTRY_SELECTOR("A glob pattern that will select the entry to extract if it matches the entry name", String.class, null);


  private final String desc;
  private final Class<?> clazz;
  private final Object defaultValue;

  ArchiveDataPathAttribute(String description, Class<?> clazz, Object defaultValue) {

    this.desc = description;
    this.clazz = clazz;
    this.defaultValue = defaultValue;
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
    return this.defaultValue;
  }

}
