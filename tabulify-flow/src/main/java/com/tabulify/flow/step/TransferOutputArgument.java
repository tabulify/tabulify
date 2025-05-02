package com.tabulify.flow.step;

import com.tabulify.conf.AttributeValue;

/**
 * On filter step, by default, the target are passed through,
 * but you may choose to pass the result of the step instead
 */
public enum TransferOutputArgument implements AttributeValue {

  TARGETS("The created output (targets) will be passed through"),
  RESULTS("The result of the transfer will be passed through"),
  SOURCES("The source of the transfer are passed through");


  private final String description;

  TransferOutputArgument(String description) {
    this.description = description;
  }


  @Override
  public String getDescription() {
    return this.description;
  }


}
