package net.bytle.timer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class Timer {



  private final String name;

  public static Timer createFromUuid() {
    return new Timer(UUID.randomUUID().toString());
  }

  @Override
  public String toString() {
    return name;
  }

  private Instant startTime;
  private Instant endTime;

  private long SECONDS_IN_MILLI = 1000;
  private long MINUTES_IN_MILLI = 1000 * 60;
  private long HOURS_IN_MILLI = 1000 * 60 * 60;


  private Long responseTimeInMs;

  private Timer(String name) {
    this.name = name;
    startTime = Instant.now();
  }

  static public Timer create(String name) {
    return new Timer(name);
  }


  public String getName() {

    return name;
  }

  public Timer start() {
    startTime = Instant.now();
    return this;
  }

  public void stop() {

    if (responseTimeInMs == null) {
      endTime = Instant.now();
      responseTimeInMs = ChronoUnit.MILLIS.between(startTime, endTime);
    } else {
      throw new IllegalStateException("The timer was already stopped");
    }

  }


  public long getResponseTimeInMilliSeconds() {
    if (responseTimeInMs == null) {
      stop();
    }
    return responseTimeInMs;
  }

  /**
   * @return the response time in (hour:minutes:seconds.milli)
   */

  public String getResponseTimeInString() {

    long elapsedHours = getResponseTimeInMilliSeconds() / HOURS_IN_MILLI;

    long diff = getResponseTimeInMilliSeconds() % HOURS_IN_MILLI;
    long elapsedMinutes = diff / MINUTES_IN_MILLI;

    diff = diff % MINUTES_IN_MILLI;
    long elapsedSeconds = diff / SECONDS_IN_MILLI;

    diff = diff % SECONDS_IN_MILLI;
    long elapsedMilliSeconds = diff;

    return elapsedHours + ":" + elapsedMinutes + ":" + elapsedSeconds + "." + elapsedMilliSeconds;

  }

  public Instant getStartTime() {
    return startTime;
  }

  public Instant getEndTime() {
    return endTime;
  }
}
