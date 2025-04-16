package net.bytle.email.flow.flow;

import com.tabulify.flow.Granularity;
import net.bytle.type.Attribute;
import net.bytle.type.Key;

public enum SendmailStepArgument implements Attribute {

  TARGET_URI("The email target connection", String.class, null),
  FROM("The email sender address", String.class, null),
  FROM_NAME("The name of the sender", String.class, null),
  TO("The email receiver address(es)", String.class, null),
  TO_NAMES("The names of the receivers", String.class, null),
  CC("The carbon copy addresses", String.class, null),
  CC_NAMES("The carbon copy names", String.class, null),
  BCC("The blind carbon copy addresses", String.class, null),
  BCC_NAMES("The blind carbon copy names", String.class, null),
  SUBJECT("The email subject", String.class, null),
  TXT("The email body in text format", String.class, null),
  HTML("The email body in html format", String.class, null),
  BODY_TYPE("The type of resources seen as email body if selected", String.class, null),
  STEP_GRANULARITY("The granularity of the run", Granularity.class, Granularity.RECORD),
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
