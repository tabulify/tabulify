package com.tabulify.template.flow;


import com.tabulify.conf.AttributeValue;

public enum TemplateEngine implements AttributeValue {


  NATIVE("The native template engine"),
  THYMELEAF("The thymeleaf template engine"),
  PEBBLE("The pebble template engine");

  private final String description;


  TemplateEngine(String description) {

    this.description = description;

  }


  @Override
  public String getDescription() {
    return this.description;
  }

}
