package net.bytle.db;

import net.bytle.fs.Fs;
import net.bytle.type.Attribute;
import net.bytle.type.Casts;

import java.nio.file.Path;

/**
 * All static configuration
 * <p>
 */
public enum TabularAttributes implements Attribute {


  /**
   * Meta directory
   */
  APP_NAME( "The name of the app", false, "tabli", String.class),
  USER_CONF_DIR_NAME( "The user configuration directory name", false, ".tabli", String.class),
  USER_CONF_DIR_PATH( "The user home configuration directory", false, Fs.getUserHome().resolve(TabularAttributes.USER_CONF_DIR_NAME.getDefaultValue().toString()), Path.class),
  PROJECT_CONF_DIR_NAME( "The project configuration directory name", false, "conf", String.class),
  VARS_FILE_NAME( "The variables file name", false, "variables.yml", String.class),
  CONNECTION_VAULT_NAME( "The name of the connection vault file", false, "connections.ini", String.class),
  USER_VARIABLES_FILE( "The location of the user variables file", true, Casts.castSafe(USER_CONF_DIR_PATH.getDefaultValue(), Path.class).resolve(VARS_FILE_NAME.getDefaultValue().toString()), Path.class),
  USER_CONNECTION_VAULT( "The location of the user connection vault file", true, Casts.castSafe(USER_CONF_DIR_PATH.getDefaultValue(), Path.class).resolve(CONNECTION_VAULT_NAME.getDefaultValue().toString()), Path.class),
  IS_DEV( "If Tabulify runs in a dev mode", true, true, Boolean.class),
  DEFAULT_FILE_SYSTEM_TABULAR_TYPE("The default file extension for tabular data", true, "csv", String.class),
  TABULIFY_HOME_PATH("The directory home of the Tabulify installation", true, null, Path.class),
  PROJECT_ENV( "The project environment", false, null, String.class),
  PROJECT_CONNECTION( "The project connection file", false, null, String.class),
  PROJECT_VARIABLE( "The project variable file", false, null, String.class),
  ENCRYPTED_VARIABLES("The list of the variables that should have their value automatically encrypted", true, null, String.class),

  // This is just a feature of the cli library for the options
  // Not really a tabular option
  LOG_LEVEL("The tabli log level", true, "info", String.class);


  private final String description;
  private final Boolean publik;
  private final Class<?> valueClazz;
  private final Object value;

  TabularAttributes(String description, boolean publik, Object defaultValue, Class<?> valueClazz) {
    this.description = description;
    this.publik = publik;
    this.value = defaultValue;
    this.valueClazz = valueClazz;
  }


  /**
   *
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
