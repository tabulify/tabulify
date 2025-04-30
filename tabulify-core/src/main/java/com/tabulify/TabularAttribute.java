package com.tabulify;

import net.bytle.type.Attribute;

import java.nio.file.Path;

/**
 * All static configuration
 * <p>
 */
public enum TabularAttribute implements Attribute {


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
  SMTP_HOST("Smtp Host Server", true, "localhost", String.class),
  SMTP_PORT("Smtp Port", true, 25, Integer.class),
  SMTP_FROM("The default from address if none is provided", true, 25, String.class),
  SMTP_FROM_NAME("The default name from address if none is provided", true, "", String.class),
  SMTP_TO("The default to address if none is provided", true, "", String.class),
  SMTP_TO_NAMES("The default names to address if none is provided", true, "", String.class),
  SMTP_AUTH("Smtp server authentication required?", true, false, Boolean.class),
  SMTP_TLS("Smtp Tls communication required", true, false, Boolean.class),
  SMTP_USER("Smtp Connection User", true, "", String.class),
  SMTP_PWD("Smtp Connection Password", true, "", String.class),
  SMTP_DEBUG("Smtp Debug level", true, "", String.class);

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
