package com.tabulify.jdbc;

import com.tabulify.connection.ConnectionAttributeEnum;

import java.sql.DatabaseMetaData;

/**
 * Attributes that comes from:
 * * {@link DatabaseMetaData}
 * * {@link DatabaseMetaData#getClientInfoProperties()}
 */
public enum SqlConnectionAttributeEnum implements ConnectionAttributeEnum {


  CONNECTION_INIT_SCRIPT("A script that runs after a connection has been established", false, true, null, String.class),
  CONNECTION_CLOSING_SCRIPT("A script that runs before a connection is closed", false, true, null, String.class),
  DATABASE_PRODUCT_NAME("The name of the database", true, false, null, String.class),
  DATABASE_PRODUCT_VERSION("The version of the database", true, false, null, String.class),
  DATABASE_MAJOR_VERSION("The major version number of the database", true, false, null, String.class),
  DATABASE_MINOR_VERSION("The minor version number of the database", true, false, null, String.class),
  JDBC_MAJOR_VERSION("The major version number of JDBC", true, false, null, String.class),
  JDBC_MINOR_VERSION("The minor version number of JDBC", true, false, null, String.class),
  DRIVER_VERSION("The driver version", true, false, null, String.class),
  DRIVER_NAME("The driver name", true, false, null, String.class),
  SUPPORT_NAMED_PARAMETERS("If the system supports named parameters in the SQL statement", true, false, null, String.class),
  SUPPORT_BATCH_UPDATES("If the system supports batch SQL updates", true, false, null, String.class),
  BUILDER_CACHE_ENABLED("Enable or disable the builder cache", false, true, true, Boolean.class),
  NAME_QUOTING_ENABLED("Enable quoting of names", false, true, true, Boolean.class),
  NAME_QUOTING_DISABLED_CASE("The case to apply when quoting is disabled", false, true, SqlNameCaseNormalization.UPPERCASE, SqlNameCaseNormalization.class);


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
  SqlConnectionAttributeEnum(String description, boolean connectionNeeded, boolean isParameter, Object defaultValue, Class<?> valueClazz) {

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
