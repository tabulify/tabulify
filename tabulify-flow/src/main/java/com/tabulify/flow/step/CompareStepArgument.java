package com.tabulify.flow.step;

import com.tabulify.conf.AttributeEnum;

public enum CompareStepArgument implements AttributeEnum {

  SOURCE("The content source that will be compared",CompareStepSource.class, CompareStepSource.CONTENT),
  REPORT("The type of compare report", CompareStepReportType.class, CompareStepReportType.RESOURCE),
  DRIVER_COLUMNS("The columns that drive the comparison", CompareStepReportType.class, CompareStepReportType.RESOURCE),
  ;

  private final String description;
  private final Class<?> clazz;
  private final Object defaultValue;

  CompareStepArgument(String description, Class<?> aClass, Object defaultValue) {
    this.description = description;
    this.clazz = aClass;
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
