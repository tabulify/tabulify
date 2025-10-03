package com.tabulify.fs.sql;

public enum SqlSubset {

  DML,
  DDL,
  TRANSACTION,
  COMMENT,
  AUDIT,
  PROCEDURE,
  STATISTICS,
  LOG,
  DCL,
  EXPLAIN,
  SCRIPT_COMMENT,
  PLSQL,
  /**
   * Database Cli Console statement
   */
  CLI,
  /**
   * SQL Command
   */
  COMMAND,
  UNKNOWN,


}
