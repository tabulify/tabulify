package net.bytle.db.spi;

import net.bytle.type.AttributeValue;

public enum AttributeProperties implements AttributeValue {

  /**
   * attribute and not property because the product is called `tabulify`
   */
  ATTRIBUTE("The name of the variable"),
  VALUE("The value of an variable"),
  DESCRIPTION("The description of the variable");


  private final String description;

  AttributeProperties(String description) {
    this.description = description;
  }


  @Override
  public String getDescription() {
    return this.description;
  }


  @Override
  public String toString() {
    return super.toString().toLowerCase();
  }

}
