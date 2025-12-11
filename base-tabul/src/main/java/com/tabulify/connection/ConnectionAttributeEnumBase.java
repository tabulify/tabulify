package com.tabulify.connection;

import com.tabulify.type.DnsName;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.UriEnhanced;

import java.util.Map;

/**
 * All built-in connection attribute
 */
public enum ConnectionAttributeEnumBase implements ConnectionAttributeEnum {

  /**
   * Name is a key normalizer so that we don't get any problem
   * such `/` and space are not supported because in ini file they may define a hierarchy and create then several datastore
   * <a href="http://ini4j.sourceforge.net/tutorial/IniTutorial.java.html">...</a>
   */
  NAME("The name of the connection", KeyNormalizer.class, null, false),
  ORIGIN("The origin of the connection", ObjectOrigin.class, null, false),
  URI("The uri of the connection", UriEnhanced.class, null, true),
  USER("The user name", String.class, null, true),
  WORKING_PATH("The working path (Schema for database, directory for file system)", String.class, null, true),
  // password and not pwd because this is the jdbc name in connection properties
  PASSWORD("The user password", String.class, null, true),
  // comment and not description because comment is the column name in a relational database
  COMMENT("A connection description", String.class, null, true),
  DATE_DATA_TYPE("Date data type used to store date values", ConnectionAttValueTimeDataType.class, ConnectionAttValueTimeDataType.NATIVE, true),
  TIMESTAMP_DATA_TYPE("Timestamp data type used to store timestamp values", ConnectionAttValueTimeDataType.class, ConnectionAttValueTimeDataType.NATIVE, true),
  TIME_DATA_TYPE("Time format data type to store time values", ConnectionAttValueTimeDataType.class, ConnectionAttValueTimeDataType.NATIVE, true),
  BOOLEAN_DATA_TYPE("Boolean data type used to store boolean values", ConnectionAttValueBooleanDataType.class, ConnectionAttValueBooleanDataType.Native, true),
  VARCHAR_DEFAULT_PRECISION("Default VARCHAR precision", Integer.class, 0, true),
  MAX_NAME_IN_PATH("The maximum number of names in a path", Integer.class, null, true),
  MAX_CONCURRENT_THREAD("The maximum number of threads that can be created against the system", Integer.class, null, true),
  NATIVES("Native Driver attributes (jdbc properties, ...)", Map.class, null, true), NVARCHAR_DEFAULT_PRECISION("Default NVARCHAR precision", Integer.class, 0, true),
  /**
   * Why 1 as default for char/nchar
   * by default, `create foo(bar char)` will create a char with length 1
   * * mySQL: no doc but tested
   * * postgres: If character (or char) lacks a specifier, it is equivalent to character(1).
   * <a href="https://www.postgresql.org/docs/current/datatype-character.html">...</a>
   */
  NCHAR_DEFAULT_PRECISION("Default NCHAR precision", Integer.class, 1, true),
  CHAR_DEFAULT_PRECISION("Default CHAR precision", Integer.class, 1, true),
  ;

  private final String description;
  private final Class<?> clazz;
  private final Object defaultValue;
  private final boolean isParameter;


  ConnectionAttributeEnumBase(String description, Class<?> valueClazz, Object defaultValue, boolean isParameter) {
    this.description = description;
    this.clazz = valueClazz;
    this.defaultValue = defaultValue;
    this.isParameter = isParameter;
  }

  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.clazz;
  }

  @Override
  public Object getDefaultValue() {
    return this.defaultValue;
  }

  @Override
  public boolean isParameter() {
    return this.isParameter;
  }


}
