package com.tabulify.flow.operation;

import com.tabulify.conf.AttributeValue;

public enum DiffPipelineStepDataOrigin implements AttributeValue {

  RECORD("The diff operation will be performed on the content"),
  STRUCTURE("The diff operation will be performed on the structure"),
  ATTRIBUTES("The diff operation will be performed on the data resources attributes");


  private final String description;

  DiffPipelineStepDataOrigin(String description) {

    this.description = description;

  }


  @Override
  public String getDescription() {
    return this.description;
  }
}
