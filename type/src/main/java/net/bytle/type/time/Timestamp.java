package net.bytle.type.time;

import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

/**
 * A wrapper around a {@link LocalDateTime local date time}
 * to represent a date with time (ie SQL Timestamp definition)
 */
public class Timestamp {

  public static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();
  LocalDateTime localDateTime;


  public Timestamp(LocalDateTime localDateTime) {
    this.localDateTime = localDateTime;
  }


  public static Timestamp createFromString(String s) throws TimeException {

    String pattern = TimeStringParser.detectFormat(s);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
    LocalDateTime dateTime;
    try {
      dateTime = LocalDateTime.parse(s, formatter);
    } catch (Exception e) {
      throw new TimeException("The string (" + s + ") is not a date", e);
    }
    return createFromLocalDateTime(dateTime);

    /**
     * Deleted 2022-10-18 to be able to handle more than millis that you can found in {@link LocalDateTime#toString()}
     * `
     * SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
     * java.util.Date date = simpleDateFormat.parse(s);
     * `
     */


  }

  public static Timestamp createFromDate(java.util.Date date) {

    LocalDateTime localDateTime = Instant.ofEpochMilli(date.getTime()).atZone(DEFAULT_ZONE_ID).toLocalDateTime();
    return new Timestamp(localDateTime);

  }

  public static Timestamp createFromNow() {
    return new Timestamp(LocalDateTime.now());
  }

  public static Timestamp createFromObject(Object sourceObject) throws TimeException {
    if (sourceObject == null) {
      return new Timestamp(null);
    } else if (sourceObject instanceof Timestamp) {
      return (Timestamp) sourceObject;
    } else if (sourceObject instanceof java.sql.Timestamp) {
      return createFromSqlTimestamp((java.sql.Timestamp) sourceObject);
    } else if (sourceObject instanceof LocalDateTime) {
      return createFromLocalDateTime((LocalDateTime) sourceObject);
    } else if (sourceObject instanceof LocalDate) {
      return createFromLocalDate((LocalDate) sourceObject);
    } else if (sourceObject instanceof Long) {
      return createFromEpochMilli((Long) sourceObject);
    } else if (sourceObject instanceof String) {
      return createFromString((String) sourceObject);
    } else if (sourceObject instanceof Date) {
      return createFromDate((Date) sourceObject);
    } else if (sourceObject instanceof Integer) {
      return createFromEpochMilli(((Integer) sourceObject).longValue());
    } else {
      throw new TimeException("The value (" + sourceObject + ") with the class (" + sourceObject.getClass().getSimpleName() + ") cannot be transformed to a timestamp");
    }
  }


  private static Timestamp createFromLocalDate(LocalDate sourceObject) {
    return new Timestamp(LocalDateTime.of(sourceObject, LocalTime.MIN));
  }

  public static Timestamp createFromSqlTimestamp(java.sql.Timestamp sourceObject) {
    return new Timestamp(sourceObject.toLocalDateTime());
  }

  public static Timestamp createFromLocalDateTime(LocalDateTime sourceObject) {
    return new Timestamp(sourceObject);
  }

  public static Timestamp createFromTimestamp(Timestamp timestamp) {
    return new Timestamp(timestamp.toLocalDateTime());
  }

  public static Timestamp createFromEpochSec(Long epochSec) {
    return createFromEpochMilli(epochSec * 1000);
  }

  public static Timestamp createFromInstant(Instant instant) {
    return new Timestamp(LocalDateTime.ofInstant(instant, DEFAULT_ZONE_ID));
  }

  public static Timestamp createFromNowUtc() {

    return createFromLocalDateTime(LocalDateTime.now(ZoneOffset.UTC));
  }

  public java.sql.Timestamp toSqlTimestamp() {

    return java.sql.Timestamp.valueOf(localDateTime);

  }

  public static Timestamp createFromEpochMilli(long epochMilli) {

    return new Timestamp(
      Instant
        .ofEpochMilli(epochMilli)
        .atZone(DEFAULT_ZONE_ID)
        .toLocalDateTime()
    );

  }


  public Long toEpochMilli() {

    return localDateTime.atZone(DEFAULT_ZONE_ID).toInstant().toEpochMilli();

  }

  public Date toDate() {
    return Date.from(localDateTime.atZone(DEFAULT_ZONE_ID).toInstant());
  }


  public String toIsoString() {
    return localDateTime.toString();
  }

  public String toString(String format) {
    SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
    return sdf.format(toDate());
  }

  public String toString(String format, TimeZone timeZone) {
    SimpleDateFormat sdf = new SimpleDateFormat(format);
    sdf.setTimeZone(timeZone);
    return sdf.format(toDate());
  }

  public LocalDateTime toLocalDateTime() {
    return this.localDateTime;
  }

  public Timestamp afterMillis(long l) {
    return new Timestamp(this.localDateTime.plus(l, ChronoUnit.MILLIS));
  }

  public Timestamp beforeMillis(long l) {
    return new Timestamp(this.localDateTime.minus(l, ChronoUnit.MILLIS));
  }

  public Boolean isAfterThan(Timestamp timestamp) {
    return this.localDateTime.isAfter(timestamp.toLocalDateTime());
  }


  public Boolean isBeforeThan(Timestamp timestamp) {
    return this.localDateTime.isBefore(timestamp.toLocalDateTime());
  }

  @Override
  public String toString() {
    return this.localDateTime.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Timestamp timestamp = (Timestamp) o;
    return Objects.equals(localDateTime, timestamp.localDateTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(localDateTime);
  }

  /**
   * @param timestamp the external timestamp
   * @return The minus result in millisecond (ie between value)
   */
  public long minus(Timestamp timestamp) {
    return between(timestamp);
  }

  public long between(Timestamp timestamp) {
    return ChronoUnit.MILLIS.between(this.localDateTime, timestamp.toLocalDateTime());
  }

  public Long toEpochSec() {
    return toEpochMilli() / 1000;
  }

  /**
   * @return A sql timestamp truncated to the second
   */
  public java.sql.Timestamp toSqlTimestampSec() {
    return java.sql.Timestamp.valueOf(localDateTime.truncatedTo(ChronoUnit.SECONDS));
  }

  public Timestamp truncate(TemporalUnit temporalUnit) {
    this.localDateTime = this.localDateTime.truncatedTo(temporalUnit);
    return this;
  }

  public java.sql.Timestamp toSqlTimestampMillis() {
    return java.sql.Timestamp.valueOf(localDateTime.truncatedTo(ChronoUnit.MILLIS));
  }

  public LocalDateTime toLocalDateTimeMillis() {
    return this.localDateTime.truncatedTo(ChronoUnit.MILLIS);
  }

  @SuppressWarnings("unused")
  public OffsetDateTime toOffsetDateTime() {
    return this.localDateTime.atOffset(OffsetDateTime.now().getOffset());
  }

  /**
   * @return a timestamp "YYYYMMDDHHMMSS" suitable for the file system
   */
  public String toFileSystemString() {
    /**
     * An ISO string has `:` that is not permitted
     */
    return toString("YYYYMMDDHHMMSS");
  }
}
