package com.tabulify.flow;


import com.tabulify.conf.AttributeValue;

public enum Granularity implements AttributeValue {


  RECORD("The record is the container unit. Example: a json or xml document in a record"),
  RESOURCE("The whole resource is the container unit.");

  private final String description;


  Granularity( String description) {
    this.description = description;
  }


  @Override
  public String getDescription() {
    return this.description;
  }
}
