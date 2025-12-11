package com.tabulify.type.time;

import com.tabulify.exception.CastException;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Calendar;

public class DateTest {

  @Test
  public void toSqlDateTest() {

    Date now = Date.createFromNow();
    java.sql.Date localDateNow = java.sql.Date.valueOf(LocalDate.now());
    Assert.assertEquals("equal", now.toSqlDate(), localDateNow);

  }

  @Test
  public void toStringIsoTest() {
    Date date = Date.createFromEpochMilli(System.currentTimeMillis());
    Assert.assertEquals("The iso format is the good one",LocalDate.now().toString(),date.toString("yyyy-MM-dd"));
    Assert.assertEquals("The iso format is the good one",LocalDate.now().toString(),date.toIsoString());
  }

  @Test
  public void createFromString() throws CastException {

    Date fromString = Date.createFromString("2020-10-12");
    String actual = fromString.toIsoString();
    Assert.assertEquals("equal", "2020-10-12", actual);

    Calendar instance = Calendar.getInstance();
    java.util.Date date = fromString.toDate();
    instance.setTime(date);
    Assert.assertEquals(0, instance.get(Calendar.HOUR));
    Assert.assertEquals(0, instance.get(Calendar.MINUTE));
    Assert.assertEquals(0, instance.get(Calendar.SECOND));


  }


}
