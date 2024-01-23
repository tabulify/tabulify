package net.bytle.vertx.analytics.event;

public class SignUpEvent extends AnalyticsServerEvent {


  public static SignUpEvent create() {
    return new SignUpEvent();
  }

  @Override
  public String getName() {
    return "Sign Up";
  }


}
