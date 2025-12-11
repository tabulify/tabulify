package com.tabulify.json;

import com.tabulify.conf.AttributeEnum;

enum JsonDataAttributes implements AttributeEnum {

  STRUCTURE("How the JSON is returned (as one JSON column or as a table with the column being the first level properties", JsonStructure.class, JsonStructure.DOCUMENT);


  private final String description;
  private final Class<?> clazz;
  private final Object defaultValue;

  JsonDataAttributes(String description, Class<?> clazz, Object defaultValue) {

    this.description = description;
    this.clazz = clazz;
    this.defaultValue = defaultValue;

  }


  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.clazz;
  }

  @Override
  public Object getDefaultValue() {
    return this.defaultValue;
  }


}
