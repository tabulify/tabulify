package com.tabulify.flow.engine;

import com.tabulify.conf.AttributeValue;

public enum FlowStepAttribute implements AttributeValue {

  NAME("The name of the operation"),
  OPERATION("The operation name"),
  ARGUMENTS("The args property name"),
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
