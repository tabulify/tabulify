package net.bytle.db.flow.step;

import net.bytle.type.AttributeValue;

public enum CompareStepSource implements AttributeValue {

  CONTENT("The compare operation will be performed on the content"),
  STRUCTURE("The compare operation will be performed on the structure"),
  ATTRIBUTE("The compare operation will be performed on the data resources attributes");


  private final String description;

  CompareStepSource(String description) {

    this.description = description;
  }


  @Override
  public String getDescription() {
    return this.description;
  }
}
