package net.bytle.type;

import java.sql.Date;
import java.time.Instant;

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

  public static Date now() {
    return new Date(Instant.now().toEpochMilli());
  }


}
