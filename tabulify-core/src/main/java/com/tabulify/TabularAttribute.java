package com.tabulify;

import com.tabulify.conf.AttributeEnum;

import java.nio.file.Path;

/**
 * All static configuration
 * <p>
 */
public enum TabularAttribute implements AttributeEnum {


  /**
   * Meta directory
   */
  ENV("The execution environment", true, TabularExecEnv.DEV, TabularExecEnv.class),
  DEFAULT_FILE_SYSTEM_TABULAR_TYPE("The default file extension for tabular data", true, "csv", String.class),
  HOME("The directory home of the Tabulify installation", true, null, Path.class),
  CONF("The path to the conf file", true, null, Path.class),
  PROJECT_HOME("The project home path", false, null, Path.class),
  PASSPHRASE("The passphrase", false, null, String.class),
  LOG_LEVEL("The tabli log level", true, "info", String.class),
  //
  // By default, the user home (trick to not show the user in the path)
  SQLITE_HOME("Sqlite home (Where to store the sqlite database)", false, null, String.class),
  NATIVE_DRIVER("Native Drivers Properties", false, null, String.class);


  private final String description;
  private final Boolean publik;
  private final Class<?> valueClazz;
  private final Object value;

  TabularAttribute(String description, boolean publik, Object defaultValue, Class<?> valueClazz) {
    this.description = description;
    this.publik = publik;
    this.value = defaultValue;
    this.valueClazz = valueClazz;
  }


  /**
   * @return if this variable can be seen by user
   */
  @SuppressWarnings("unused")
  boolean isPublic() {
    return publik;
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
