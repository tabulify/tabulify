package com.tabulify.type.time;

import com.tabulify.exception.CastException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

public class TimestampTest {


  @Test
  public void DateUtilTest() {

    Date now = new Date();
    Timestamp timestamp = Timestamp.createFromDate(now);
    Date dateFromTimestamp = timestamp.toDate();
    Assert.assertEquals("They are equals", now, dateFromTimestamp);

  }

  @Test
  public void comparisonTest() {
    Timestamp now = Timestamp.createFromNowLocalSystem();
    Timestamp lower = now.beforeMillis(1000);
    Assert.assertNotEquals("Not equals",now,lower);
    Assert.assertTrue("Before",lower.isBeforeThan(now));
  }

  @Test
  public void fromString() throws CastException {
    Timestamp timestamp = Timestamp.createFromNowLocalSystem();
    String timestampString = timestamp.toString();
    Timestamp newTimestamp = Timestamp.createFromString(timestampString);
    Assert.assertEquals(timestamp,newTimestamp);
  }

}
