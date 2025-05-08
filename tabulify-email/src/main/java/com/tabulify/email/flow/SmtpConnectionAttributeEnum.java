package com.tabulify.email.flow;

import com.tabulify.connection.ConnectionAttributeEnum;


public enum SmtpConnectionAttributeEnum implements ConnectionAttributeEnum {


  FROM("The default from address if none is provided", null, String.class, true),
  FROM_NAME("The default name from address if none is provided", "", String.class, true),
  TO("The default to address if none is provided", "", String.class, true),
  TO_NAMES("The default names to address if none is provided", "", String.class, true),
  CC("The default carbon copy addresses if none is provided", "", String.class, true),
  CC_NAMES("The default carbon copy names to address if none is provided", "", String.class, true),
  BCC("The default blind carbon copy addresses", null, null, true),
  BCC_NAMES("The default blind carbon copy names", null, null, true),
  AUTH("Smtp server authentication required?", false, Boolean.class, true),
  TLS("Smtp TLS communication required", false, Boolean.class, true),
  DEBUG("Smtp Debug", false, Boolean.class, true);

  private final String description;
  private final Class<?> clazz;
  private final Object defaultValue;
  private final boolean isParameter;


  SmtpConnectionAttributeEnum(String description, Object defaultValue, Class<?> valueClazz, boolean isParameter) {
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
