package net.bytle.vertx.analytics;

import net.bytle.type.Strings;

/**
 * The analytics event
 */
public class AnalyticsEventName {


  public static final AnalyticsEventName SIGN_IN = new AnalyticsEventName("Sign In");

  /**
   * When a user sign up
   */
  public static AnalyticsEventName SIGN_UP = new AnalyticsEventName("Sign Up");

  /**
   * When a user is created by import
   * (ie mailing list import)
   */
  public static AnalyticsEventName SIGN_UP_VIA_IMPORT = new AnalyticsEventName("Sign Up Import");

  private final String name;

  AnalyticsEventName(String name) {

    this.name = Strings.createFromString(name).toCapitalize().toString();

  }

  public static AnalyticsEventName createFromEvent(String eventName) {
    return new AnalyticsEventName(eventName);
  }

  public String toCamelCase() {
    return this.name;
  }

  /**
   *
   * @return the event name as slug - that can be used as name in the file system
   */
  public String toFileSystemName() {
    return name.trim().toLowerCase().replace(" ", "-");
  }

  @Override
  public String toString() {
    return this.name;
  }



}
