package com.tabulify.transfer;

import com.tabulify.conf.AttributeValue;

public enum TransferColumnMapping implements AttributeValue {

  MAP_BY_POSITION("A map by position was given"),
  MAP_BY_NAME("A map by name was given"),
  NAME("The columns are mapped by name"),
  POSITION("The columns are mapped by position");


  private final String description;

  TransferColumnMapping(String description) {
    this.description = description;
  }


  @Override
  public String getDescription() {
    return this.description;
  }
}
