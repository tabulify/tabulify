package com.tabulify.fs.sql;

import com.tabulify.type.KeyInterface;
import com.tabulify.type.KeyNormalizer;

public enum FsSqlParserColumn implements KeyInterface {

  /**
   * The name of th statement
   * (ie CREATE, ...)
   */
  NAME,
  /**
   * Type
   * DDL, DML, DCL, ...
   */
  SUBSET,
  /**
   * SQL, PSQL, SQL_PLUS, COMMENT
   */
  CATEGORY,
  /**
   * The content
   */
  SQL,
  ;

  private final KeyNormalizer normalizer;

  FsSqlParserColumn() {
    this.normalizer = KeyNormalizer.createSafe(this.name());
  }


  @Override
  public KeyNormalizer toKeyNormalizer() {
    return normalizer;
  }
}
