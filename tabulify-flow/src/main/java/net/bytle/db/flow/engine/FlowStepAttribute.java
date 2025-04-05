package net.bytle.db.flow.engine;

import net.bytle.type.AttributeValue;

public enum FlowStepAttribute implements AttributeValue {

  NAME("The name of the operation"),
  OPERATION("The operation name"),
  ARGUMENTS("The argument name"),
  DESCRIPTION("A step description"),
  ;

  private final String comment;

  FlowStepAttribute(String description) {

    this.comment = description;

  }

  @Override
  public String getDescription() {
    return this.comment;
  }


}
