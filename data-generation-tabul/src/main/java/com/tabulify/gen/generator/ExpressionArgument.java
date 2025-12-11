package com.tabulify.gen.generator;

public enum ExpressionArgument {

  COLUMN_VARIABLES("A list of column names used in the expression"),
  COLUMN_VARIABLE("A column name used in the expression"),
  EXPRESSION("The expression");

  private final String desc;

  ExpressionArgument(String desc) {
    this.desc = desc;
  }

  public String getDesc() {
    return desc;
  }
}
