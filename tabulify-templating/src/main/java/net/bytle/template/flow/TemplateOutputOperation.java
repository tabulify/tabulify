package net.bytle.template.flow;


import net.bytle.type.AttributeValue;

public enum TemplateOutputOperation implements AttributeValue {


  TEMPLATES("The templates result are the output (one resource by template)"),
  EXTENDED_RECORDS( "The source record are passed through with extra-columns that stores the template results");


  private final String description;


  TemplateOutputOperation( String description) {

    this.description = description;

  }


  @Override
  public String getDescription() {
    return this.description;
  }

}
