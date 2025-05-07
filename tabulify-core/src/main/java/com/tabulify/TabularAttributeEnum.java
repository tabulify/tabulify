package com.tabulify;

import com.tabulify.conf.AttributeEnumParameter;

import java.nio.file.Path;

/**
 * All static configuration
 * <p>
 */
public enum TabularAttributeEnum implements AttributeEnumParameter {


  /**
   * Meta directory
   */
  ENV("The execution environment", true, TabularExecEnv.DEV, TabularExecEnv.class),
  DEFAULT_FILE_SYSTEM_TABULAR_TYPE("The default file extension for tabular data", true, "csv", String.class),
  HOME("The directory home of the Tabulify installation", true, null, Path.class),
  CONF("The path to the conf file", true, null, Path.class),
  PROJECT_HOME("The project home path", false, null, Path.class),
  PASSPHRASE("The passphrase", false, null, String.class),
  LOG_LEVEL("The log level", true, TabularLogLevel.WARN, TabularLogLevel.class),
  SQLITE_HOME("Sqlite home (Where to store the sqlite database)", false, null, String.class),
  NATIVE_DRIVER("Native Drivers Properties", false, null, String.class);


  private final String description;
  private final Boolean parameter;
  private final Class<?> valueClazz;
  private final Object value;

  TabularAttributeEnum(String description, boolean parameter, Object defaultValue, Class<?> valueClazz) {
    this.description = description;
    this.parameter = parameter;
    this.value = defaultValue;
    this.valueClazz = valueClazz;
  }


  /**
   * @return if this variable can be seen by user
   */
  @Override
  public boolean isParameter() {
    return parameter;
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
    return this.value;
  }


}
