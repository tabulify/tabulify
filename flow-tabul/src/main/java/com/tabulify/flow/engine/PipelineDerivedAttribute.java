package com.tabulify.flow.engine;

import com.tabulify.conf.AttributeEnum;
import com.tabulify.flow.operation.PipelineStepProcessingType;

import java.sql.Timestamp;

public enum PipelineDerivedAttribute implements AttributeEnum {

  START_TIME("The start time", Timestamp.class),
  PROCESSING_TYPE("The processing type", PipelineStepProcessingType.class),
  LOGICAL_NAME("The logical name", String.class),
  ;

  private final String description;
  private final Class<?> clazz;


  PipelineDerivedAttribute(String description, Class<?> clazz) {
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
