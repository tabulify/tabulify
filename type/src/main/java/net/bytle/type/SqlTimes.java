package net.bytle.type;

import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class SqlTimes {

  public static Time fromString(String s){
    try {
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat(UtilDate.getFormat(s));
      java.util.Date date = simpleDateFormat.parse(s);
      return fromDateUtil(date);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  public static Time fromDateUtil(java.util.Date date){
    return new java.sql.Time(date.getTime());
  }


}
