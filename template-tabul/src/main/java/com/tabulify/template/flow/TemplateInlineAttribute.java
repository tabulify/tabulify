package com.tabulify.template.flow;

public enum TemplateInlineAttribute {

  MEDIA_TYPE("The media type of the template"),
  CONTENT("The content"),
  LOGICAL_NAME("The data definition of the template"),
  ;

  private final String desc;

  TemplateInlineAttribute(String description) {
    this.desc = description;
  }

  public String getDesc() {
    return desc;
  }
}
