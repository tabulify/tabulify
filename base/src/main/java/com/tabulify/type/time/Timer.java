package com.tabulify.type.time;

import java.time.Duration;
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

    if (endTime != null) {
      throw new IllegalStateException("The timer was already stopped");
    }
    endTime = Instant.now();

    responseTimeInMs = ChronoUnit.MILLIS.between(startTime, endTime);

  }

  public boolean hasStopped() {
    return endTime != null;
  }



  public Duration getDuration() {
    if (responseTimeInMs == null) {
      stop();
    }
    return Duration.of(responseTimeInMs, ChronoUnit.MILLIS);
  }

  public Instant getStartTime() {
    return startTime;
  }

  public Instant getEndTime() {
    return endTime;
  }
}
