package net.bytle.tower.eraldy.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.vertx.analytics.AnalyticsServerEvent;

public class SignUpEvent extends AnalyticsServerEvent {


  private Integer flowId;

  public static SignUpEvent create() {
    return new SignUpEvent();
  }

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
