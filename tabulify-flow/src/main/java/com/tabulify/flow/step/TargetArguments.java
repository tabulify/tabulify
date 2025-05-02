package com.tabulify.flow.step;

import com.tabulify.conf.AttributeEnum;

/**
 * The arguments used in a {@link TargetFilterStepAbs target operation}
 */
public enum TargetArguments implements AttributeEnum {

  TARGET_DATA_URI( "defines the target data URI destination (Example: [table]@connection). If the target data uri has no name, the name will be the name of the source.", true),
  TARGET_DATA_DEFINITION( "The data definition of the target", false);

  private final String description;
  private final Boolean mandatory;



  TargetArguments(String description, Boolean mandatory) {

    this.description = description;
    this.mandatory = mandatory;

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


  public Boolean getMandatory() {
    return mandatory;
  }

}
