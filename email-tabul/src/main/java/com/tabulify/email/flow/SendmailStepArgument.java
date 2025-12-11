package com.tabulify.email.flow;

import com.tabulify.conf.AttributeEnum;
import com.tabulify.flow.Granularity;
import com.tabulify.type.Key;

public enum SendmailStepArgument implements AttributeEnum {

  TARGET_URI("The email target connection", String.class, null),
  FROM("The email sender Internet Address", String.class, null),
  TO("The email receiver Internet Address(es)", String.class, null),
  CC("The carbon copy Internet Address(es)", String.class, null),
  BCC("The blind carbon Internet Address(es)", String.class, null),
  SUBJECT("The email subject", String.class, null),
  TXT("The email body in text format", String.class, null),
  HTML("The email body in html format", String.class, null),
  BODY_TYPE("The type of resources seen as email body if selected", String.class, null),
  GRANULARITY("The granularity of the execution", Granularity.class, Granularity.RESOURCE),
  LOG_TARGET_URI("The target uri where to write the logs", String.class, null);


  private final String description;
  private final Class<?> clazz;
  private final Object defVal;
  private final String columnName;


  SendmailStepArgument(String description, Class<?> clazz, Object defVal) {

    this.description = description;
    this.clazz = clazz;
    this.defVal = defVal;
    this.columnName = Key.toColumnName(this.toString());
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
    return this.defVal;
  }

  public String toColumnName() {
    return this.columnName;
  }
}
