package net.bytle.type;

import java.sql.Date;
import java.time.Instant;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * Static function on the {@link Date} data type
 * SqlDate is a wrapper that stores date as milli
 */
public class SqlDates {

  /**
   * Return a date from an epoch millisecond
   * @param epochMilli
   * @return
   */
  public static Date fromEpochMilli(Long epochMilli) {

    return new Date(epochMilli);
  }

  /**
   * Retrieve a date for now
   * @return
   */
  public static Date now() {
    return new Date(Instant.now().toEpochMilli());
  }


  /**
   *
   * @param from
   * @param to
   * @return the number of days between this two dates
   */
  public static Long dayBetween(Date from, Date to) {
    return DAYS.between(from.toLocalDate(),to.toLocalDate());
  }

}
