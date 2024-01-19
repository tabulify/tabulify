package net.bytle.vertx.analytics;

import net.bytle.type.Strings;

/**
 * The analytics event name/type
 */
public class AnalyticsEventName {



  private final String name;

  AnalyticsEventName(String name) {

    this.name = Strings.createFromString(name).toCapitalize().normalizeWhiteSpaceToOnlyOneConsecutive().toString();

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
