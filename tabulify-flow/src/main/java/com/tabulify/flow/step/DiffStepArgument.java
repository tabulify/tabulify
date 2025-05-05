package com.tabulify.flow.step;

import com.tabulify.conf.AttributeEnum;

public enum DiffStepArgument implements AttributeEnum {

  SOURCE("The content source that will be compared", DiffStepSource.class, DiffStepSource.CONTENT),
  REPORT("The type of compare report", DiffStepReportType.class, DiffStepReportType.RESOURCE),
  DRIVER_COLUMNS("The columns that drive the comparison", DiffStepReportType.class, DiffStepReportType.RESOURCE),
  ;

  private final String description;
  private final Class<?> clazz;
  private final Object defaultValue;

  DiffStepArgument(String description, Class<?> aClass, Object defaultValue) {
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
