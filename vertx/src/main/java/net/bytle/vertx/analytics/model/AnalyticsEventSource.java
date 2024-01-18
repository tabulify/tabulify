package net.bytle.vertx.analytics.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
* Where the request was created (server, browser or mobile) (known also as the Channel)
*/
public enum AnalyticsEventSource {

  SERVER(0),

  API(1),

  NUMBER_2(2);

  private final Integer value;

  AnalyticsEventSource(Integer value) {
    this.value = value;
  }

  @JsonValue
  public Integer getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public static AnalyticsEventSource fromValue(Integer value) {
    for (AnalyticsEventSource b : AnalyticsEventSource.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }

}
