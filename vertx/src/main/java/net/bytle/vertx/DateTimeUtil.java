package net.bytle.vertx;

import net.bytle.type.time.Timestamp;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {

  static public DateTimeFormatter defaultFormat() {
    return DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  }

  static public String LocalDateTimetoString(LocalDateTime localDateTime) {
    return localDateTime.format(defaultFormat());
  }

  /**
   * @return the now time in UTC
   */
  public static LocalDateTime getNowInUtc() {
    return Timestamp.createFromNowUtc().toLocalDateTime();
  }


}
