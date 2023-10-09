package net.bytle.type.time;

import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

import static java.time.temporal.ChronoUnit.DAYS;
import static net.bytle.type.time.TimeStringParser.detectFormat;

/**
 * A wrapper around all date format where Date means day precision
 * ie YYYY-MM-DD
 */
@SuppressWarnings("unused")
public class Date {

  private static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault(); // ZoneId.of("GMT")
  private final LocalDate localDate;

  /**
   * @return a local date on the gmt zone
   * same as {@link LocalDate#now()} but more explicit
   */
  public static Date createFromNow() {

    return new Date(LocalDate.now(DEFAULT_ZONE_ID));

  }

  public Date(LocalDate localDate) {
    this.localDate = localDate;
  }

  public static Date createFromEpochMilli(Long epochMilli) {

    return new Date((new java.sql.Date(epochMilli)).toLocalDate());

  }

  public static Date createFromSqlDate(java.sql.Date date) {
    return new Date(date.toLocalDate());
  }

  public static Date createFromEpochDay(Long epochDay) {
    return new Date(LocalDate.ofEpochDay(epochDay));
  }

  public static Date createFromObject(Object o) {
    if (o instanceof Date) {
      return (Date) o;
    } else if (o instanceof LocalDate) {
      return new Date((LocalDate) o);
    } else if (o instanceof Long) {
      return createFromEpochDay((Long) o);
    } else if (o instanceof java.sql.Date) {
      return createFromSqlDate((java.sql.Date) o);
    } else if (o instanceof java.util.Date) {
      return createFromDate((java.util.Date) o);
    } else if (o instanceof String) {
      return createFromString((String) o);
    } else if (o instanceof Integer) {
      return createFromEpochDay(((Integer) o).longValue());
    } else {
      throw new IllegalArgumentException("The object (" + o + ") has an class (" + o.getClass().getSimpleName() + ") that is not yet seen as a date.");
    }
  }

  public static Date createFromEpochSec(Long epochSec) {
    return createFromEpochMilli(epochSec*1000);
  }

  @SuppressWarnings("unused")
  public static Date createFromFileTime(FileTime time) {
    return new Date(LocalDate.from(time.toInstant()));
  }


  /**
   * Return a {@link java.sql.Date} YYYY-MM-DD
   *
   * @return the date in SQL format
   */
  public java.sql.Date toSqlDate() {

    return java.sql.Date.valueOf(localDate);

  }


  /**
   * @param to the date
   * @return the number of days to
   * <p>
   * Positive if from < to
   * and that the date is not below 1970 (new Date(0L) = 1970-01-01
   * Otherwise negative
   */
  public long daysTo(Date to) {
    return DAYS.between(localDate, to.toLocalDate());
  }

  /**
   * @param from the date
   * @return the number of days from the
   * <p>
   * Positive if from < to
   * and that the date is not below 1970 (new Date(0L) = 1970-01-01
   * Otherwise negative
   */
  public long daysFrom(Date from) {
    return DAYS.between(from.toLocalDate(), localDate);
  }

  public LocalDate toLocalDate() {
    return this.localDate;
  }


  public static Date createFromString(String s) {
    try {
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat(detectFormat(s));
      java.util.Date date = simpleDateFormat.parse(s);
      return createFromDate(date);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * A java.util.date is a time milli second  precision without time zone
   *
   * @param date - from an date util
   * @return a date
   */
  public static Date createFromDate(java.util.Date date) {
    LocalDate localDate = date.toInstant().atZone(DEFAULT_ZONE_ID).toLocalDate();
    return new Date(localDate);
  }

  public String toString(String format) {
    SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
    return sdf.format(toDate());
  }

  public java.util.Date toDate() {
    return java.sql.Date.from(localDate.atStartOfDay(DEFAULT_ZONE_ID).toInstant());
  }


  public String toIsoString() {
    return localDate.toString();
  }

  public Date plusDays(long daysToAdd) {
    return new Date(localDate.plusDays(daysToAdd));
  }

  public Date minusDays(long daysToAdd) {
    return new Date(localDate.minusDays(daysToAdd));
  }


  public Long toEpochDay() {
    return localDate.toEpochDay();
  }

  public Long toEpochMillis() {
    return toEpochDay() * 1000 * 60 * 60 * 24;
  }


  /**
   * Epoch in second
   *
   * @return the second from epoch
   */
  public Long toEpochSec() {
    return localDate.toEpochDay() * 24 * 60 * 60;
  }


}
