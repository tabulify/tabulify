package net.bytle.vertx.analytics.event;

public class UserProfileUpdateEvent extends AnalyticsServerEvent {


  @Override
  public String getName() {
    return "User Profile Update";
  }


}
