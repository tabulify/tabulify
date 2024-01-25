package net.bytle.vertx.analytics.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * The properties of a user agent
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsAgent   {


  protected String id;

  protected String string;

  protected String timezoneId;

  protected String locale;

  protected String osName;

  protected String osVersion;

  protected String osArch;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public AnalyticsAgent () {
  }

  /**
  * @return id The user agent/device id (a uuid4 or a fingerprint for instance)  It's also known as the user anonymous id because this is the user identifier when the user is unknown.  When you send a message, a userId or a deviceId is required in order to identify a user.  We prefer the term device id (mixpanel) over anonymous id (segment) because it's more meaningful. We track the device.  It's created on the client side and stored by the client more permanently (via a cookie for the browser and stored in local storage for durability).  It's not the same as a session id because: * a session id may be regenerated (when the user sign in for instance). * the device id does not have any security feature. You can't login
  */
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  /**
  * @param id The user agent/device id (a uuid4 or a fingerprint for instance)  It's also known as the user anonymous id because this is the user identifier when the user is unknown.  When you send a message, a userId or a deviceId is required in order to identify a user.  We prefer the term device id (mixpanel) over anonymous id (segment) because it's more meaningful. We track the device.  It's created on the client side and stored by the client more permanently (via a cookie for the browser and stored in local storage for durability).  It's not the same as a session id because: * a session id may be regenerated (when the user sign in for instance). * the device id does not have any security feature. You can't login
  */
  @SuppressWarnings("unused")
  public void setId(String id) {
    this.id = id;
  }

  /**
  * @return string The user agent string
  */
  @JsonProperty("string")
  public String getString() {
    return string;
  }

  /**
  * @param string The user agent string
  */
  @SuppressWarnings("unused")
  public void setString(String string) {
    this.string = string;
  }

  /**
  * @return timezoneId the timezone id (ie Europe/London)
  */
  @JsonProperty("timezoneId")
  public String getTimezoneId() {
    return timezoneId;
  }

  /**
  * @param timezoneId the timezone id (ie Europe/London)
  */
  @SuppressWarnings("unused")
  public void setTimezoneId(String timezoneId) {
    this.timezoneId = timezoneId;
  }

  /**
  * @return locale the locale (in a browser, this is the language setting)
  */
  @JsonProperty("locale")
  public String getLocale() {
    return locale;
  }

  /**
  * @param locale the locale (in a browser, this is the language setting)
  */
  @SuppressWarnings("unused")
  public void setLocale(String locale) {
    this.locale = locale;
  }

  /**
  * @return osName the OS name
  */
  @JsonProperty("osName")
  public String getOsName() {
    return osName;
  }

  /**
  * @param osName the OS name
  */
  @SuppressWarnings("unused")
  public void setOsName(String osName) {
    this.osName = osName;
  }

  /**
  * @return osVersion the OS version
  */
  @JsonProperty("osVersion")
  public String getOsVersion() {
    return osVersion;
  }

  /**
  * @param osVersion the OS version
  */
  @SuppressWarnings("unused")
  public void setOsVersion(String osVersion) {
    this.osVersion = osVersion;
  }

  /**
  * @return osArch the OS architecture
  */
  @JsonProperty("osArch")
  public String getOsArch() {
    return osArch;
  }

  /**
  * @param osArch the OS architecture
  */
  @SuppressWarnings("unused")
  public void setOsArch(String osArch) {
    this.osArch = osArch;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnalyticsAgent analyticsAgent = (AnalyticsAgent) o;
    return

            Objects.equals(id, analyticsAgent.id) && Objects.equals(string, analyticsAgent.string) && Objects.equals(timezoneId, analyticsAgent.timezoneId) && Objects.equals(locale, analyticsAgent.locale) && Objects.equals(osName, analyticsAgent.osName) && Objects.equals(osVersion, analyticsAgent.osVersion) && Objects.equals(osArch, analyticsAgent.osArch);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, string, timezoneId, locale, osName, osVersion, osArch);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
