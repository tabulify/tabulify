package com.tabulify.fs.sql;

import com.tabulify.conf.AttributeValue;

public enum FsSqlParsingModeValue implements AttributeValue {


  SQL("return also comment, block"),
  TEXT("end of records parsing");

  private final String desc;

  FsSqlParsingModeValue(String description) {
    this.desc = description;
  }

  public String getDescription() {
    return desc;
  }
}
