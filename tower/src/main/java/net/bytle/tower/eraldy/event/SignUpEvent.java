package net.bytle.tower.eraldy.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.vertx.analytics.model.AnalyticsEvent;

public class SignUpEvent extends AnalyticsEvent {


  private Integer flowId;

  @Override
  public String getName() {
    return "Sign Up";
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
