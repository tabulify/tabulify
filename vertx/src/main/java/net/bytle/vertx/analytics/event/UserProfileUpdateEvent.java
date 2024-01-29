package net.bytle.vertx.analytics.event;

public class UserProfileUpdateEvent extends AnalyticsServerEvent {


  @Override
  public AnalyticsEventType getType() {
    return AnalyticsEventType.USER_UPDATE;
  }


}
