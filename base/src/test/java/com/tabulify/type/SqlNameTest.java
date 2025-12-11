package com.tabulify.type;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SqlNameTest {


  /**
   * Test a relative path as input
   */
  @Test
  void relativePath() {

    String sqlName = "../../../../../tmp/9301769192504729337/12473184776600725892.sql_cd_postgres";
    String validSqlName = SqlName.create(sqlName).toValidSqlName();
    Assertions.assertEquals("tmp_9301769192504729337_12473184776600725892_sql_cd_postgres", validSqlName);

  }

  @Test
  void firstLetterDigit() {

    String sqlName = "9foo";
    String validSqlName = SqlName.create(sqlName).toValidSqlName();
    Assertions.assertEquals("a9foo", validSqlName);

  }
}
