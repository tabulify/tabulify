package com.tabulify.diff;

import com.tabulify.conf.AttributeEnum;

public enum DataComparisonAttribute implements AttributeEnum {

  COMP_ID("The record id of the comparison report"),
  COMP_ORIGIN("The origin of the record (source or target)"),
  COMP_COMMENT("A quick description about the difference"),
  COMP_DIFF_ID("The id of the difference. Two records with the same id have been compared"),
  COMP_ORIGIN_ID("The natural id of the origin if an unique column was not defined");

  private final String comment;


  DataComparisonAttribute(String comment) {
    this.comment = comment;
  }


  @Override
  public String getDescription() {
    return this.comment;
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
