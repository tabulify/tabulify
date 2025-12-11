package com.tabulify.template.flow;


import com.tabulify.conf.AttributeValue;

public enum TemplateTargetType implements AttributeValue {


  TEMPLATE_OUTPUT("The templates result are the output (one resource by template)"),
  DATA_MODEL("The data model"),
  ENRICHED_INPUT("The inputs record are passed through with extra-columns that stores the template results");


  private final String description;


  TemplateTargetType(String description) {

    this.description = description;

  }


  @Override
  public String getDescription() {
    return this.description;
  }

}
