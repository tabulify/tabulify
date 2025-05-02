package com.tabulify.jdbc;

import com.tabulify.connection.ConnectionAttributeEnum;

import java.sql.DatabaseMetaData;

/**
 * Attributes that comes from:
 *   * {@link DatabaseMetaData}
 *   * {@link DatabaseMetaData#getClientInfoProperties()}
 *
 */
public enum SqlConnectionAttributeEnum implements ConnectionAttributeEnum {


  CONNECTION_INIT_SCRIPT("A script that runs after a connection has been established", false, true),
  CONNECTION_CLOSING_SCRIPT("A script that runs before a connection is closed", false, true),
  DATABASE_PRODUCT_NAME("The name of the database", true, false),
  DATABASE_PRODUCT_VERSION("The version of the database", true, false),
  DATABASE_MAJOR_VERSION("The major version number of the database", true, false),
  DATABASE_MINOR_VERSION("The minor version number of the database", true, false),
  JDBC_MAJOR_VERSION("The major version number of JDBC", true, false),
  JDBC_MINOR_VERSION("The minor version number of JDBC", true, false),
  DRIVER_VERSION("The driver version", true, false),
  DRIVER_NAME("The driver name", true, false),
  SUPPORT_NAMED_PARAMETERS("If the system supports named parameters in the SQL statement", true, false),
  SUPPORT_BATCH_UPDATES("If the system supports batch SQL updates", true, false);


  private final String description;
  private final boolean needsConnection;
  private final boolean isParameter;

  /**
   * @param description      - the description of the variable
   * @param connectionNeeded - if the connection is needed to get the value
   * @param isParameter - if the value can be set by the user
   */
  SqlConnectionAttributeEnum(String description, boolean connectionNeeded, boolean isParameter) {

    this.description = description;
    this.needsConnection = connectionNeeded;
    this.isParameter = isParameter;

  }

  boolean needsConnection(){
    return this.needsConnection;
  }



  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public Class<?> getValueClazz() {
    return String.class;
  }

  @Override
  public Object getDefaultValue() {
    return null;
  }


  @Override
  public boolean isParameter() {
    return isParameter;
  }
}
