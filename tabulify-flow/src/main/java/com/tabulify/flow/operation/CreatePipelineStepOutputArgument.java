package com.tabulify.flow.operation;

import com.tabulify.conf.AttributeValue;

/**
 * On filter step, by default, the target are passed through,
 * but you may choose to pass the result of the step instead
 */
public enum CreatePipelineStepOutputArgument implements AttributeValue {

  TARGETS("The created targets will be passed through"),
  INPUTS("The inputs"),
  RESULTS("A data resource of source/target");


  private final String description;

  CreatePipelineStepOutputArgument(String description) {
    this.description = description;
  }


  @Override
  public String getDescription() {
    return this.description;
  }


}
