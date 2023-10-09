package net.bytle.db.yaml;

import net.bytle.type.AttributeValue;

public enum YamlStyle implements AttributeValue {

  // https://yaml.org/spec/1.2.2/#chapter-6-structural-productions
  BLOCK( "The indentation yaml format (ie block style)"),
  // https://yaml.org/spec/1.2.2/#chapter-7-flow-style-productions
  JSON("The json yaml format (ie flow style)");


  private final String description;

  YamlStyle( String description) {

    this.description = description;

  }



  @Override
  public String getDescription() {
    return this.description;
  }

}
