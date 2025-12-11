package com.tabulify.type.time;

import org.junit.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.TimeZone;

public class TimeZoneTest {

  @Test
  public void listTimeZoneTest() {
    Arrays.stream(TimeZone.getAvailableIDs())
      .map(id->TimeZone.getTimeZone(id))
      .sorted(Comparator.comparing(TimeZone::getRawOffset))
      .forEach(tz-> System.out.println(+tz.getRawOffset()/1000/60/24+" - "+tz.getID()+" - "+tz.getDisplayName()));
  }
}
