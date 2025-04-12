package com.tabulify.flow.step;

import net.bytle.type.Attribute;

public enum DefineStepAttribute implements Attribute {

  DATA_RESOURCE("One data resource"),
  DATA_RESOURCES("Multiple data resources"),
  DATA_URI("Data Uri"),
  DATA("Data"),
  DATA_DEFINITION("Data definition");


  private final String description;



  DefineStepAttribute( String description) {

    this.description = description;
  }


  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public Class<?> getValueClazz() {
    return null;
  }

  @Override
  public Object getDefaultValue() {
    return null;
  }

}

