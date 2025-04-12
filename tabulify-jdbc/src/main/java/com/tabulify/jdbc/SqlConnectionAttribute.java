package com.tabulify.jdbc;

import net.bytle.type.Attribute;

import java.sql.DatabaseMetaData;

/**
 * Attributes that comes from:
 *   * {@link DatabaseMetaData}
 *   * {@link DatabaseMetaData#getClientInfoProperties()}
 *
 */
public enum SqlConnectionAttribute implements Attribute {


  CONNECTION_INIT_SCRIPT("A script that runs after a connection has been established",false),
  CONNECTION_CLOSING_SCRIPT("A script that runs before a connection is closed",false),
  DRIVER("The driver class",false),
  DATABASE_PRODUCT_NAME("The name of the database",true),
  DATABASE_PRODUCT_VERSION("The version of the database",true),
  DATABASE_MAJOR_VERSION("The major version number of the database",true),
  DATABASE_MINOR_VERSION("The minor version number of the database",true),
  JDBC_MAJOR_VERSION("The major version number of JDBC",true),
  JDBC_MINOR_VERSION("The minor version number of JDBC",true),
  DRIVER_VERSION("The driver version",true),
  DRIVER_NAME("The driver name",true),
  SUPPORT_NAMED_PARAMETERS("If the system supports named parameters in the SQL statement",true),
  SUPPORT_BATCH_UPDATES("If the system supports batch SQL updates",true);




  private final String description;
  private final boolean derived;

  /**
   *
   * @param description - the description of the variable
   * @param connectionNeeded - if the connection is needed to get the value
   */
  SqlConnectionAttribute(String description, boolean connectionNeeded) {

    this.description = description;
    this.derived = connectionNeeded;

  }

  boolean needsConnection(){
    return this.derived;
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


}
