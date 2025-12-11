package com.tabulify.gen.generator;

public enum DataSetArgument {

  DATA_URI("A mandatory data uri"),
  COLUMN("The column to get the data from in the data resource specified by the data uri"),
  META_COLUMNS("A map that links a local column to a metadata column (a third column)"),
  ;

  private final String desc;

  DataSetArgument(String desc) {
    this.desc = desc;
  }

  public String getDesc() {
    return desc;
  }
}
