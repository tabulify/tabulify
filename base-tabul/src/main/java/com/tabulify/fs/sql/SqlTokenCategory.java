package com.tabulify.fs.sql;

public enum SqlTokenCategory {

  /**
   * Statement from the sql console
   * Oracle: SqlPlus statement
   * MySql...
   * They are on one line
   */
  COMMAND,

  /**
   * Token with Sql Statement
   * They have no empty line, they always terminate with a end line character ;
   */
  SQL,

  /**
   * Token with PlSql Statement
   * They may have empty line
   */
  PSQL,

  /**
   * COMMENT Statement
   */
  COMMENT,
  UNKNOWN,

}
