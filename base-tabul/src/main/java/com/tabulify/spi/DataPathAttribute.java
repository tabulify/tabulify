package com.tabulify.spi;

import com.tabulify.conf.AttributeEnum;

import java.sql.Timestamp;
import java.util.List;

/**
 * The attributes,
 * they are also used in Yaml files to define the data path structure
 */
public enum DataPathAttribute implements AttributeEnum {


  NAME("The name of the data resource", String.class, false),
  COMMENT("A comment", String.class, true),
  LOGICAL_NAME("The logical name", String.class, true),
  PARENT("The parent", String.class, false),
  CONNECTION("The connection name", String.class, false),
  DATA_URI("The data uri", String.class, false),
  PATH("The relative path to the default connection path", String.class, false),
  ABSOLUTE_PATH("The absolute path on the data system", String.class, false),
  COUNT("The number of records", String.class, false),
  SIZE("The number of byte", Integer.class, false),
  MEDIA_TYPE("The media type", String.class, false),
  MEDIA_SUBTYPE("The media subType", String.class, false),
  KIND("The kind of media", String.class, false),
  MD5("The Md5 hash", String.class, false),
  SHA384("The Sha384 hash", String.class, false),
  SHA384_INTEGRITY("The sha384 value used in the html integrity attribute", String.class, false),
  // Private attributes (used to parse Yaml)
  COLUMNS("The columns definition (used in Yaml)", List.class, true),
  PRIMARY_COLUMNS("The primary columns definition (used in Yaml)", List.class, true),
  ACCESS_TIME("The access time (access time)", Timestamp.class, false),
  CREATION_TIME("The creation time (birth time)", Timestamp.class, false),
  UPDATE_TIME("The last update time (modify time)", Timestamp.class, false),
  TABULAR_TYPE("The tabular type", TabularType.class, true),
  ;


  /**
   * The key value that comes in the info output report
   */
  private final String description;
  private final Class<?> clazz;
  /**
   * Can the user update this attribute
   */
  private final boolean updatable;


  /**
   *
   */
  DataPathAttribute(String description, Class<?> clazz, boolean updatable) {

    this.description = description;
    this.clazz = clazz;
    this.updatable = updatable;
  }


  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public Class<?> getValueClazz() {
    return clazz;
  }

  @Override
  public Object getDefaultValue() {
    return null;
  }


  @Override
  public String toString() {
    return super.toString().toLowerCase();
  }

  @Override
  public boolean getIsUpdatable() {
    return updatable;
  }

}
