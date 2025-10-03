package com.tabulify.sqlite;

import com.tabulify.connection.ConnectionAttributeEnum;

import java.sql.DatabaseMetaData;

/**
 * Attributes that comes from:
 * * {@link DatabaseMetaData}
 * * {@link DatabaseMetaData#getClientInfoProperties()}
 */
public enum SqliteConnectionAttributeEnum implements ConnectionAttributeEnum {


  /**
   * ie smallint become integer
   */
  TYPE_AFFINITY_CONVERSION("If true, the type name are converted to affinity name", false, false, false, Boolean.class);


  private final String description;
  private final boolean needsConnection;
  private final boolean isParameter;
  private final Object defaultValue;
  private final Class<?> valueClazz;

  /**
   * @param description      - the description of the variable
   * @param connectionNeeded - if the connection is needed to get the value
   * @param isParameter      - if the value can be set by the user
   * @param defaultValue     - the default value
   * @param valueClazz       - the value clazz
   */
  SqliteConnectionAttributeEnum(String description, boolean connectionNeeded, boolean isParameter, Object defaultValue, Class<?> valueClazz) {

    this.description = description;
    this.needsConnection = connectionNeeded;
    this.isParameter = isParameter;
    this.defaultValue = defaultValue;
    this.valueClazz = valueClazz;

  }

  boolean needsConnection() {
    return this.needsConnection;
  }


  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public Class<?> getValueClazz() {
    return valueClazz;
  }

  @Override
  public Object getDefaultValue() {
    return defaultValue;
  }


  @Override
  public boolean isParameter() {
    return isParameter;
  }
}
