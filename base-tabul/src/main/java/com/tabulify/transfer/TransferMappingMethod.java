package com.tabulify.transfer;

import com.tabulify.conf.AttributeValue;
import com.tabulify.type.KeyNormalizer;

public enum TransferMappingMethod implements AttributeValue {

  MAP_BY_POSITION("A map by position was given"),
  MAP_BY_NAME("A map by name was given"),
  NAME("The columns are mapped by name"),
  POSITION("The columns are mapped by position");


  private final String description;

  TransferMappingMethod(String description) {
    this.description = description;
  }


  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public String toString() {
    return KeyNormalizer.createSafe(this.name()).toCliLongOptionName();
  }
}
