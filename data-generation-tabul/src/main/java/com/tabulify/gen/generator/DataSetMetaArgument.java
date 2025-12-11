package com.tabulify.gen.generator;

public enum DataSetMetaArgument {
  COLUMN("The column to get the data from in the data set"),
  COLUMN_DATA_SET("The column that contains a data set data supplier (ie an entity or a data set generator)");

  private final String desc;

  DataSetMetaArgument(String desc) {
    this.desc = desc;
  }

  public String getDesc() {
    return desc;
  }
}
