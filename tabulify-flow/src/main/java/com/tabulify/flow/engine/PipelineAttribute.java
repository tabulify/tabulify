package com.tabulify.flow.engine;

import com.tabulify.conf.AttributeEnum;

import java.util.List;

public enum PipelineAttribute implements AttributeEnum {

  LOGICAL_NAME("The logical Name", String.class),
  PIPELINE( "The Pipeline attribute", List.class);


  private final String description;
  private final Class<?> clazz;


  PipelineAttribute(String description, Class<?> clazz) {
    this.description = description;
    this.clazz = clazz;
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
    return null;
  }

}
