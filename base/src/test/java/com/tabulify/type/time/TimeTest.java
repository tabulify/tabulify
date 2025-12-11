package com.tabulify.type.time;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class TimeTest {

  @Test
  public void timeTest() {
    Time time = Time.createFromString("22:10:22");
    java.sql.Time expected = java.sql.Time.valueOf("22:10:22");
    Assertions.assertEquals(expected, time.toSqlTime(), "Same time");
  }

  @Test
  public void timeWithoutMilliSecond() {
    Time time = Time.createFromString("22:10");
    java.sql.Time expected = java.sql.Time.valueOf("22:10:00");
    Assertions.assertEquals(expected, time.toSqlTime(), "Same time");
  }
}
