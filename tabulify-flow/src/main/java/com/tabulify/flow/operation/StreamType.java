package com.tabulify.flow.operation;

import com.tabulify.conf.AttributeValue;


public enum StreamType implements AttributeValue {

  MAP("A map operation will produce one output for one input"),
  SPLIT("A map operation will produce multiple output for one input");


  private final String description;

  StreamType(String description) {
    this.description = description;
  }


  @Override
  public String getDescription() {
    return this.description;
  }


}
