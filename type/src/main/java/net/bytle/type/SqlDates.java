package net.bytle.type;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;

import static java.time.temporal.ChronoUnit.DAYS;
import static net.bytle.type.UtilDate.getFormat;

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
   * Wrapper around {@link LocalDate#now()}
   * @return a date without time parts (at day precision) from GMT/UTC
   */
  public static Date dateNow() {
    return Date.valueOf(LocalDates.nowGmt());
  }


  /**
   *
   * @param from
   * @param to
   * @return the number of days between this two dates
   *
   * Positive if from < to
   * and that the date is not below 1970 (new Date(0L) = 1970-01-01
   * Otherwise negative
   */
  public static long dayBetween(Date from, Date to) {
    return DAYS.between(from.toLocalDate(),to.toLocalDate());
  }



  public static Date fromString(String s) {
    try {
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat(getFormat(s));
      java.util.Date date = simpleDateFormat.parse(s);
      return fromDateUtil(date);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  public static Date fromDateUtil(java.util.Date date){
    return new java.sql.Date(date.getTime());
  }




}
