package com.tabulify.email.flow;

import com.tabulify.connection.ConnectionAttribute;

public enum SmtpConnectionAttribute implements ConnectionAttribute {

  HOST("Smtp Host Server", "localhost", String.class),
  PORT("Smtp Port", 25, Integer.class),
  FROM("The default from address if none is provided", 25, String.class),
  FROM_NAME("The default name from address if none is provided", "", String.class),
  TO("The default to address if none is provided", "", String.class),
  TO_NAMES("The default names to address if none is provided", "", String.class),
  CC("The default carbon copy addresses if none is provided", "", String.class),
  CC_NAMES("The default carbon copy names to address if none is provided", "", String.class),
  BCC("The blind carbon copy addresses", null, null),
  BCC_NAMES("The blind carbon copy names", null, null),
  AUTH("Smtp server authentication required?", false, Boolean.class),
  TLS("Smtp Tls communication required", false, Boolean.class),
  DEBUG("Smtp Debug level", "", String.class);

  private final String description;
  private final Class<?> clazz;
  private final Object defaultValue;


  SmtpConnectionAttribute(String description, Object defaultValue, Class<?> valueClazz) {
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
