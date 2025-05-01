package com.tabulify.email.flow;

import com.tabulify.connection.ConnectionAttribute;

public enum SmtpConnectionAttribute implements ConnectionAttribute {

  SMTP_HOST("Smtp Host Server", "localhost", String.class),
  SMTP_PORT("Smtp Port", 25, Integer.class),
  SMTP_FROM("The default from address if none is provided", 25, String.class),
  SMTP_FROM_NAME("The default name from address if none is provided", "", String.class),
  SMTP_TO("The default to address if none is provided", "", String.class),
  SMTP_TO_NAMES("The default names to address if none is provided", "", String.class),
  SMTP_AUTH("Smtp server authentication required?", false, Boolean.class),
  SMTP_TLS("Smtp Tls communication required", false, Boolean.class),
  SMTP_DEBUG("Smtp Debug level", "", String.class),
  SMTP_USER("Smtp User", "", String.class),
  SMTP_PWD("Smtp Password", "", String.class);

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
