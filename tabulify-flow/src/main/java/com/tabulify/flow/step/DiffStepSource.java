package com.tabulify.flow.step;

import com.tabulify.conf.AttributeValue;

public enum DiffStepSource implements AttributeValue {

  CONTENT("The compare operation will be performed on the content"),
  STRUCTURE("The compare operation will be performed on the structure"),
  ATTRIBUTE("The compare operation will be performed on the data resources attributes");


  private final String description;

  DiffStepSource(String description) {

    this.description = description;
  }


  @Override
  public String getDescription() {
    return this.description;
  }
}
