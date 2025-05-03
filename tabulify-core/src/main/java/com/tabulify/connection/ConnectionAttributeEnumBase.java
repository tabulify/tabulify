package com.tabulify.connection;

import java.util.Map;

/**
 * All built-in connection attribute
 */
public enum ConnectionAttributeEnumBase implements ConnectionAttributeEnum {

  NAME("The name of the connection", String.class, null, false),
  ORIGIN("The origin of the connection", ConnectionOrigin.class, null, false),
  URI("The uri of the connection", String.class, null, true),
  USER("The user name to login", String.class, null, true),
  // password and not pwd because this is the jdbc name
  PASSWORD("The user password to login", String.class, null, true),
  DESCRIPTION("A connection description", String.class, null, true),
  DATE_DATA_TYPE("Date data type used to store date values", ConnectionAttValueTimeDataType.class, ConnectionAttValueTimeDataType.NATIVE, true),
  TIMESTAMP_DATA_TYPE("Timestamp data type used to store timestamp values", ConnectionAttValueTimeDataType.class, ConnectionAttValueTimeDataType.NATIVE, true),
  TIME_DATA_TYPE("Time format data type to store time values", ConnectionAttValueTimeDataType.class, ConnectionAttValueTimeDataType.NATIVE, true),
  BOOLEAN_DATA_TYPE("Boolean data type used to store boolean values", ConnectionAttValueBooleanDataType.class, ConnectionAttValueBooleanDataType.Native, true),
  MAX_NAME_IN_PATH("The maximum number of names in a path", Integer.class, null, true),
  MAX_CONCURRENT_THREAD("The maximum number of threads that can be created against the system", Integer.class, null, true),
  NATIVES("Native Driver attributes (jdbc properties, ...)", Map.class, null, true),
  /**
   * jdbc driver is here and not in the sql connection because
   * we use it in {@link ConnectionHowTos}
   */
  DRIVER("The driver class", String.class, null, true);

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
