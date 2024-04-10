package net.bytle.vertx;

import net.bytle.type.time.Timestamp;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeService {

  /**
   * Z = UTC, we work local at UTC
   * We accept UTC time and send UTC time
   * ie UTC->LocalDateTime->UTC
   * The default is the Javascript format(without the nnnnn)
   * because this is the client
   * <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date#date_time_string_format">...</a>
   */
  private static final String TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_PATTERN);


  static public String defaultFormat() {

    return TIME_PATTERN;
  }


  static public DateTimeFormatter defaultFormatter() {

    return TIME_FORMATTER;
  }

  static public String LocalDateTimetoString(LocalDateTime localDateTime) {

    return localDateTime.format(defaultFormatter());

  }

  /**
   * @return the now time in UTC
   */
  public static LocalDateTime getNowInUtc() {
    return Timestamp.createFromNowUtc().toLocalDateTime();
  }


}
