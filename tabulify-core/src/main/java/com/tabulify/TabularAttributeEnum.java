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
  EXEC_ENV("The execution environment", true, TabularExecEnv.DEV, TabularExecEnv.class),
  HOME("The directory of the Tabulify installation", true, null, Path.class),
  CONF("The conf vault file path", true, null, Path.class),
  APP_HOME("The app home directory (default to the " + Tabular.TABUL_CONF_FILE_NAME + " file directory)", true, null, Path.class),
  PASSPHRASE("The passphrase", true, null, String.class),
  LOG_LEVEL("The log level", true, TabularLogLevel.WARN, TabularLogLevel.class),
  USER_HOME("Tabulify User home Directory", true, null, Path.class),
  /**
   * Strict is when there is a discrepancy with what we expect and that's not an error
   * We throw then but gives the user the possibility to bypass it
   */
  STRICT_EXECUTION("Strict mode (Fail ambiguous situation)", true, true, Boolean.class);


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
