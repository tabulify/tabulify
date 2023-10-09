package net.bytle.db.json;

import net.bytle.type.AttributeValue;

public enum JsonStructure implements AttributeValue {


  DOCUMENT("The returned data path will be a JSON column containing the whole document"),
  PROPERTIES("The first level JSON properties will be transformed as tabular data");


  private final String description;

  JsonStructure(String description) {
    this.description = description;
  }

  @Override
  public String getDescription() {
    return this.description;
  }

}
