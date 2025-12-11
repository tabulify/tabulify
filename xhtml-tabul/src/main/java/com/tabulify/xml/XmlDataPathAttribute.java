package com.tabulify.xml;

import com.tabulify.conf.AttributeEnum;

enum XmlDataPathAttribute implements AttributeEnum {

  COLUMN_NAME("The name of the column when the JSON is returned in one column"),
  ;

  private final String description;

  XmlDataPathAttribute(String description) {

    this.description = description;
  }

  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public Class<?> getValueClazz() {
    return String.class;
  }

  @Override
  public Object getDefaultValue() {
    return null;
  }


}
