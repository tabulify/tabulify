package com.tabulify;

import net.bytle.fs.Fs;
import net.bytle.type.Attribute;
import net.bytle.type.Casts;

import java.nio.file.Path;

/**
 * All static configuration
 * <p>
 */
public enum TabularAttribute implements Attribute {


  /**
   * Meta directory
   */
  APP_NAME("The name of the app", false, "tabli", String.class),
  USER_CONF_DIR_NAME("The user configuration directory name", false, ".tabli", String.class),
  USER_CONF_DIR_PATH("The user home configuration directory", false, Fs.getUserHome().resolve(TabularAttribute.USER_CONF_DIR_NAME.getDefaultValue().toString()), Path.class),
  PROJECT_CONF_DIR_NAME("The project configuration directory name", false, "conf", String.class),
  VARS_FILE_NAME("The variables file name", false, "variables.yml", String.class),
  CONNECTION_VAULT("The path to the connection vault file", false, "connections.ini", String.class),
  USER_VARIABLES_FILE("The location of the user variables file", true, Casts.castSafe(USER_CONF_DIR_PATH.getDefaultValue(), Path.class).resolve(VARS_FILE_NAME.getDefaultValue().toString()), Path.class),
  USER_CONNECTION_VAULT("The location of the user connection vault file", true, Casts.castSafe(USER_CONF_DIR_PATH.getDefaultValue(), Path.class).resolve(CONNECTION_VAULT.getDefaultValue().toString()), Path.class),
  IS_DEV("If Tabulify runs in a dev mode", true, true, Boolean.class),
  ENV("The execution environment", true, TabularExecEnv.DEV, TabularExecEnv.class),
  DEFAULT_FILE_SYSTEM_TABULAR_TYPE("The default file extension for tabular data", true, "csv", String.class),
  HOME("The directory home of the Tabulify installation", true, null, Path.class),
  CONF("The path to the conf file", true, null, Path.class),

  // This is just a feature of the cli library for the options
  // Not really a tabular option
  LOG_LEVEL("The tabli log level", true, "info", String.class),

  PASSPHRASE("The passphrase", false, null, String.class),

  PROJECT_HOME("The project home path", false, null, Path.class),

  // Where to store the sqlite database
  // By default, the user home (trick to not show the user in the path)
  SQLITE_HOME("Sqlite home", false, null, String.class),
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
   * @return if this variable can be overwritten by the environment
   */
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
