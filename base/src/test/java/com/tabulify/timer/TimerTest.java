package com.tabulify.timer;

import com.tabulify.type.time.DurationShort;
import com.tabulify.type.time.Timer;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.time.Duration;

public class TimerTest {


  @Test
  public void baseTest() throws InterruptedException {

    Timer timer = Timer
      .create("test")
      .start();
    Thread.sleep(100);
    timer.stop();

    Duration duration = timer.getDuration();
    System.out.println(duration);
    DurationShort durationShort = DurationShort.builder().build(duration);
    String isoString = durationShort.toFormatBuilder().setFormat(DurationShort.DurationFormat.TIME_ISO).build();
    System.out.println(isoString);
    Assertions.assertTrue(isoString.contains(":"));
    String defaultFormat = durationShort.toFormatBuilder().setFormat(DurationShort.DurationFormat.DURATION_LONG).build();
    System.out.println(defaultFormat);
    Assertions.assertTrue(defaultFormat.contains("milli"));

  }
}
