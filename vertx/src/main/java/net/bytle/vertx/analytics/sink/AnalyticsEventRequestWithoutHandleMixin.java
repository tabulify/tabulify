package net.bytle.vertx.analytics.sink;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class delete the handle properties
 */
public abstract class AnalyticsEventRequestWithoutHandleMixin {


  @JsonIgnore
  @JsonProperty("requestFlowHandle")
  abstract String getRequestFlowHandle();


}
