package com.tabulify.type.yaml;

import com.tabulify.text.yaml.DefaultTimestampWithoutTimeZoneConstructor;
import org.junit.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;

public class DefaultTimestampWithoutTimeZoneConstructorTest {

  /**
   * <a href="https://yaml.org/type/timestamp.html">...</a>
   */
  @Test
  public void testTimestamp() {
    Yaml yaml = new Yaml(new DefaultTimestampWithoutTimeZoneConstructor(new LoaderOptions()));

    /**
     * When the string has no timestamp
     */
    String timestamp = "2001-09-09T01:46:40";
    LocalDateTime localDateTime = LocalDateTime.parse(timestamp);
    LocalDateTime time = yaml.load(timestamp);
    assertEquals(localDateTime, time);
    /**
     * Otherwise default
     */
    String timezoneTs = "2001-12-14 21:59:43.10 -5";
    Object date = yaml.load(timezoneTs);
    assertEquals(java.util.Date.class, date.getClass());
  }

}
