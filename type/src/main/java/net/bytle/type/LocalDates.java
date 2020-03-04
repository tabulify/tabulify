package net.bytle.type;

import java.time.LocalDate;
import java.time.ZoneId;

public class LocalDates {

  /**
   *
   * @return a local date on the gmt zone
   * same as {@link LocalDate#now()} but more explicit
   */
  public static LocalDate nowGmt(){
    return LocalDate.now(ZoneId.of("GMT"));
  }

}
