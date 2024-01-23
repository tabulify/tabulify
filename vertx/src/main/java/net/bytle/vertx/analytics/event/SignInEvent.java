package net.bytle.vertx.analytics.event;

public class SignInEvent extends AnalyticsServerEvent {


  @Override
  public String getName() {
    return "Sign In";
  }

}
