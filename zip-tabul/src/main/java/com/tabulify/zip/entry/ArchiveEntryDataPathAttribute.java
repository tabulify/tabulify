package com.tabulify.zip.entry;

import com.tabulify.conf.AttributeEnum;

public enum ArchiveEntryDataPathAttribute implements AttributeEnum {


  ENTRY_PATH("The entry path in the archive", String.class);

  private final String description;
  private final Class<?> clazz;

  ArchiveEntryDataPathAttribute(String description, Class<?> aClass) {
    this.description = description;
    this.clazz = aClass;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public Object getDefaultValue() {
    return null;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.clazz;
  }

}
