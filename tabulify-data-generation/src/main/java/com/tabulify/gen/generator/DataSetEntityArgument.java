package com.tabulify.gen.generator;

public enum DataSetEntityArgument {

  NAME("The name of the entity"),
  COLUMN("The name of the column to the data from the entity"),
  LOCALE("The locale of the entity for entity localization"),
  META_COLUMNS("A map that links a local column to a metadata entity column");

  private final String description;

  DataSetEntityArgument(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
