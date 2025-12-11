package com.tabulify.type.time;

import com.tabulify.exception.CastException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Period;
import java.util.concurrent.TimeUnit;


class DurationShortTest {

  @Test
  void durationBaseline() throws CastException {

    /**
     * Unit is mandatory
     */
    Assertions.assertThrows(java.time.format.DateTimeParseException.class, () -> Duration.parse("PTS"));

    /**
     * Second
     */
    Duration duration = DurationShort.create("0.500s").getDuration();
    Assertions.assertEquals(duration, Duration.parse("PT0.500S"));
    Assertions.assertEquals(500, duration.toMillis());
    // PT0S -> 0 seconds
    duration = DurationShort.create("0s").getDuration();
    Assertions.assertEquals(duration, Duration.parse("PT0S"));
    Assertions.assertEquals(0, duration.getSeconds());
    // PT3S -> 3 seconds
    Assertions.assertEquals(DurationShort.create("3s").getDuration(), Duration.parse("PT3S"));
    // PT2.5S -> 2.5 seconds
    Assertions.assertEquals(DurationShort.create("2.5s").getDuration(), Duration.parse("PT2.5S"));
    // 3s
    Assertions.assertEquals(DurationShort.create("3S").getDuration(), Duration.parse("PT3S"));
    // Already formatted strings work too
    Assertions.assertEquals(DurationShort.create("PT10S").getDuration(), Duration.parse("PT10S"));


    /**
     * Minutes
     */
    Assertions.assertEquals(DurationShort.create("5M").getDuration(), Duration.parse("PT5M"));
    /**
     * Hours
     */
    // 2 hours
    Assertions.assertEquals(DurationShort.create("2H").getDuration(), Duration.parse("PT2H"));
    // PT1H30M -> 1.5 hours
    Assertions.assertEquals(DurationShort.create("1H30M").getDuration(), Duration.parse("PT1H30M"));
    Assertions.assertEquals(DurationShort.create("1D").getDuration(), Duration.parse("P1D"));
    /**
     * Day
     */
    // P1DT2H -> 1 day 2 hours (mixed)
    Assertions.assertEquals(DurationShort.create("1D2H").getDuration(), Duration.parse("P1DT2H"));

    /**
     * Week
     */
    // week is also only supported in period
    Assertions.assertThrows(java.time.format.DateTimeParseException.class, () -> Duration.parse("P3W"));
    Period period = Period.parse("P3W");
    Assertions.assertEquals(3 * 7, period.getDays());

    /**
     * Month
     */
    // month is not supported by duration because month is variable (maybe 28 or 31 days)
    Assertions.assertThrows(java.time.format.DateTimeParseException.class, () -> Duration.parse("P2M3D"));
    Assertions.assertThrows(java.time.format.DateTimeParseException.class, () -> Duration.parse("P1Y2M3D"));

    // Use Period for date-based durations with months/years
    period = Period.parse("P2M3D");
    Assertions.assertEquals(3, period.getDays());
    Assertions.assertEquals(2, period.getMonths());

  }

  @Test
  void buildFromTimeUnit() {
    DurationShort durationShort = DurationShort.builder().build(1000, TimeUnit.MILLISECONDS);
    Assertions.assertEquals("1 second",durationShort.toFormatBuilder().setFormat(DurationShort.DurationFormat.DURATION_LONG).build());
  }

  @Test
  void toHumanString() {
    DurationShort durationShort = DurationShort.builder().build(1500, TimeUnit.MILLISECONDS);
    Assertions.assertEquals("1 second 500 milliseconds",durationShort.toFormatBuilder().setFormat(DurationShort.DurationFormat.DURATION_LONG).build());

    durationShort = DurationShort.builder().build(500, TimeUnit.MILLISECONDS);
    Assertions.assertEquals("500 milliseconds",durationShort.toFormatBuilder().setFormat(DurationShort.DurationFormat.DURATION_LONG).build());
  }

  @Test
  void toDurationIso() {
    DurationShort durationShort = DurationShort.builder().build(1500, TimeUnit.MILLISECONDS);
    Assertions.assertEquals("1.500s",durationShort.toFormatBuilder().setFormat(DurationShort.DurationFormat.DURATION_ISO).build());

    // second are equal to zero
    durationShort = DurationShort.builder().build(500, TimeUnit.MILLISECONDS);
    Assertions.assertEquals("0.500s",durationShort.toFormatBuilder().setFormat(DurationShort.DurationFormat.DURATION_ISO).build());
  }
}
