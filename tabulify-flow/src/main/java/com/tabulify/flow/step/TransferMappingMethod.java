package com.tabulify.flow.step;


import net.bytle.type.AttributeValue;

public enum TransferMappingMethod implements AttributeValue {

  NAME("the column mapping is done by name"),
  POSITION("the column mapping is done by position"),
  MAP("the column mapping is given by a map"),
  ;

  private final String description;

  TransferMappingMethod(String description) {
    this.description = description;

  }


  @Override
  public String getDescription() {
    return this.description;
  }


}
