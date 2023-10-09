package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * where the request originated from server, browser or mobile
 */
public enum AnalyticsEventChannel {

  SERVER("server"),

  BROWSER("browser"),

  MOBILE("mobile");

  private String value;

  AnalyticsEventChannel(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public static AnalyticsEventChannel fromValue(String value) {
    for (AnalyticsEventChannel b : AnalyticsEventChannel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
