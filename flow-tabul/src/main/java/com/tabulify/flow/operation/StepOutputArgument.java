package com.tabulify.flow.operation;

import com.tabulify.conf.AttributeValue;

/**
 * On filter step, by default, the target are passed through,
 * but you may choose to pass the result of the step instead
 * Note that results or sources may have high cardinality
 */
public enum StepOutputArgument implements AttributeValue {

  TARGETS("The targets of the step will be passed downstream"),
  RESULTS("The result of the step will be passed downstream"),
  INPUTS("The inputs resource of the transfer are passed downstream");


  private final String description;

  StepOutputArgument(String description) {
    this.description = description;
  }


  @Override
  public String getDescription() {
    return this.description;
  }


}
