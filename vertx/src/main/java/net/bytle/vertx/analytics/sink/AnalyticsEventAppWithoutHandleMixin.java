package net.bytle.vertx.analytics.sink;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class delete the handle properties
 */
public abstract class AnalyticsEventAppWithoutHandleMixin {


  @JsonIgnore
  @JsonProperty("appHandle")
  abstract String getAppHandle();

  @JsonIgnore
  @JsonProperty("appRealmHandle")
  abstract String getAppRealmHandle();

  @JsonIgnore
  @JsonProperty("appOrganizationHandle")
  abstract String getAppOrganizationHandle();


}
