package com.tabulify.fs;

import com.tabulify.connection.ConnectionAttributeEnum;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

public enum FsConnectionAttribute implements ConnectionAttributeEnum {

  /**
   * When we move tabular data into a file system,
   * the below extension (tabular format) for the file is used
   */
  TABULAR_FILE_TYPE("The default file type when downloading tabular data", true, MediaTypes.TEXT_CSV, MediaType.class);


  private final String desc;
  private final boolean isParameter;
  private final Class<?> valueClazz;
  private final Object defaultValue;

  FsConnectionAttribute(String description, boolean isParameter, Object defaultValue, Class<?> valueClazz) {
    this.desc = description;
    this.isParameter = isParameter;
    this.defaultValue = defaultValue;
    this.valueClazz = valueClazz;
  }

  @Override
  public boolean isParameter() {
    return this.isParameter;
  }

  @Override
  public String getDescription() {
    return this.desc;
  }

  @Override
  public Object getDefaultValue() {
    return this.defaultValue;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.valueClazz;
  }

}
