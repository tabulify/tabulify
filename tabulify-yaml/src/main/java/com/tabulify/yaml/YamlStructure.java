package com.tabulify.yaml;

import net.bytle.type.AttributeValue;

public enum YamlStructure implements AttributeValue {

  FILE( "One row, One column returning the file"),
  DOCUMENT("One row by document, One column"),
  PROPERTIES( "One row by document, Multiple columns (one by property)");


  private final String description;

  YamlStructure( String description) {
    this.description = description;
  }


  @Override
  public String getDescription() {
    return this.description;
  }

}
