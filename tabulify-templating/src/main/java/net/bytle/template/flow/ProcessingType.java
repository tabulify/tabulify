package net.bytle.template.flow;


import com.tabulify.conf.AttributeValue;

public enum ProcessingType implements AttributeValue {


  CUMULATIVE("N data model, 1 output"),
  MAP("1 data model, 1 output");


  private final String description;


  ProcessingType(String description) {

    this.description = description;

  }


  @Override
  public String getDescription() {
    return this.description;
  }

}
