package com.tabulify.type.time;


import com.tabulify.exception.CastException;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class TimeStringParserTest {


  @Test
  public void timeStringParserTest() {
    List<String> dates = new ArrayList<>();
    // iso timestamp
    dates.add("2020-10-14T20:46:49.868");
    // Sql timestamp
    dates.add("2020-10-14 21:03:29.779");
    dates.stream()
      .map(s-> {
        try {
          return new SimpleDateFormat(TimeStringParser.detectFormat(s)).parse(s);
        } catch (ParseException | CastException e) {
          throw new RuntimeException(e);
        }
      });

  }
}
