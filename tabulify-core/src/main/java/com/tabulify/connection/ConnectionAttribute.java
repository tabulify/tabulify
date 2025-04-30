package com.tabulify.connection;

import net.bytle.type.Attribute;

import java.util.Map;

/**
 * All built-in connection attribute
 */
public enum ConnectionAttribute implements Attribute {

  NAME("The name of the connection", String.class, null),
  ORIGIN( "The origin of the connection", ConnectionOrigin.class, null),
  URI( "The uri of the connection", String.class, null),
  USER( "The user name to login", String.class, null),
  PASSWORD( "The user password to login", String.class, null),
  DESCRIPTION( "A connection description", String.class,null),
  DATE_DATA_TYPE( "Date data type used to store date values", ConnectionAttValueTimeDataType.class, ConnectionAttValueTimeDataType.NATIVE),
  TIMESTAMP_DATA_TYPE( "Timestamp data type used to store timestamp values", ConnectionAttValueTimeDataType.class, ConnectionAttValueTimeDataType.NATIVE),
  TIME_DATA_TYPE( "Time format data type to store time values", ConnectionAttValueTimeDataType.class, ConnectionAttValueTimeDataType.NATIVE),
  BOOLEAN_DATA_TYPE("Boolean data type used to store boolean values", ConnectionAttValueBooleanDataType.class,  ConnectionAttValueBooleanDataType.Native),
  MAX_NAME_IN_PATH( "The maximum number of names in a path", Integer.class, null),
  MAX_CONCURRENT_THREAD("The maximum number of threads that can be created against the system", Integer.class, null),
  DRIVER("Jdbc Driver attributes", Map.class, null);

  private final String description;

  private final Class<?> clazz;
  private final Object defaultValue;


  ConnectionAttribute( String description, Class<?> valueClazz, Object defaultValue) {
    this.description = description;
    this.clazz = valueClazz;
    this.defaultValue = defaultValue;
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

}
