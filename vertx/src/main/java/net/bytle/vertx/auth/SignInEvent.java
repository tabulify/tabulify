package net.bytle.vertx.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.vertx.analytics.model.AnalyticsEvent;

public class SignInEvent extends AnalyticsEvent {


  private Integer flowId;

  @Override
  public String getName() {
    return "Sign In";
  }


  /**
   * The flow of sign-up
   */
  @JsonProperty("flow")
  public Integer getFlowId() {
    return this.flowId;
  }

  public void setFlowId(Integer userSource) {
    this.flowId = userSource;
  }

}
