package net.bytle.vertx.auth;

import net.bytle.vertx.analytics.AnalyticsServerEvent;

public class SignInEvent extends AnalyticsServerEvent {


  @Override
  public String getName() {
    return "Sign In";
  }

}
