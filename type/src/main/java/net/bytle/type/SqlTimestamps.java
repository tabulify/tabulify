package net.bytle.type;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class SqlTimestamps {

  public static Timestamp fromString(String s) {
    try {
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat(UtilDate.getFormat(s));
      java.util.Date date = simpleDateFormat.parse(s);
      return fromDateUtil(date);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  public static Timestamp fromDateUtil(java.util.Date date) {
    return new java.sql.Timestamp(date.getTime());
  }

}
