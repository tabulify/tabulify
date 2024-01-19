package net.bytle.vertx.analytics.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * The properties of a user agent
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsAgent   {


  protected String agentId;

  protected String agentString;

  protected String agentTimezone;

  protected String agentLocale;

  protected String agentOsName;

  protected String agentOsVersion;

  protected String agentOsArch;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public AnalyticsAgent () {
  }

  /**
  * @return agentId The user agent/device id (a uuid4 or a fingerprint for instance)  It's also known as the user anonymous id because this is the user identifier when the user is unknown.  When you send a message, a userId or a deviceId is required in order to identify a user.  We prefer the term device id (mixpanel) over anonymous id (segment) because it's more meaningful. We track the device.  It's created on the client side and stored by the client more permanently (via a cookie for the browser and stored in local storage for durability).  It's not the same as a session id because: * a session id may be regenerated (when the user sign in for instance). * the device id does not have any security feature. You can't login
  */
  @JsonProperty("agentId")
  public String getAgentId() {
    return agentId;
  }

  /**
  * @param agentId The user agent/device id (a uuid4 or a fingerprint for instance)  It's also known as the user anonymous id because this is the user identifier when the user is unknown.  When you send a message, a userId or a deviceId is required in order to identify a user.  We prefer the term device id (mixpanel) over anonymous id (segment) because it's more meaningful. We track the device.  It's created on the client side and stored by the client more permanently (via a cookie for the browser and stored in local storage for durability).  It's not the same as a session id because: * a session id may be regenerated (when the user sign in for instance). * the device id does not have any security feature. You can't login
  */
  @SuppressWarnings("unused")
  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

  /**
  * @return agentString The user agent string
  */
  @JsonProperty("agentString")
  public String getAgentString() {
    return agentString;
  }

  /**
  * @param agentString The user agent string
  */
  @SuppressWarnings("unused")
  public void setAgentString(String agentString) {
    this.agentString = agentString;
  }

  /**
  * @return agentTimezone the timezone id (ie Europe/London)
  */
  @JsonProperty("agentTimezone")
  public String getAgentTimezone() {
    return agentTimezone;
  }

  /**
  * @param agentTimezone the timezone id (ie Europe/London)
  */
  @SuppressWarnings("unused")
  public void setAgentTimezone(String agentTimezone) {
    this.agentTimezone = agentTimezone;
  }

  /**
  * @return agentLocale the locale (in a browser, this is the language setting)
  */
  @JsonProperty("agentLocale")
  public String getAgentLocale() {
    return agentLocale;
  }

  /**
  * @param agentLocale the locale (in a browser, this is the language setting)
  */
  @SuppressWarnings("unused")
  public void setAgentLocale(String agentLocale) {
    this.agentLocale = agentLocale;
  }

  /**
  * @return agentOsName the OS name
  */
  @JsonProperty("agentOsName")
  public String getAgentOsName() {
    return agentOsName;
  }

  /**
  * @param agentOsName the OS name
  */
  @SuppressWarnings("unused")
  public void setAgentOsName(String agentOsName) {
    this.agentOsName = agentOsName;
  }

  /**
  * @return agentOsVersion the OS version
  */
  @JsonProperty("agentOsVersion")
  public String getAgentOsVersion() {
    return agentOsVersion;
  }

  /**
  * @param agentOsVersion the OS version
  */
  @SuppressWarnings("unused")
  public void setAgentOsVersion(String agentOsVersion) {
    this.agentOsVersion = agentOsVersion;
  }

  /**
  * @return agentOsArch the OS architecture
  */
  @JsonProperty("agentOsArch")
  public String getAgentOsArch() {
    return agentOsArch;
  }

  /**
  * @param agentOsArch the OS architecture
  */
  @SuppressWarnings("unused")
  public void setAgentOsArch(String agentOsArch) {
    this.agentOsArch = agentOsArch;
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

            Objects.equals(agentId, analyticsAgent.agentId) && Objects.equals(agentString, analyticsAgent.agentString) && Objects.equals(agentTimezone, analyticsAgent.agentTimezone) && Objects.equals(agentLocale, analyticsAgent.agentLocale) && Objects.equals(agentOsName, analyticsAgent.agentOsName) && Objects.equals(agentOsVersion, analyticsAgent.agentOsVersion) && Objects.equals(agentOsArch, analyticsAgent.agentOsArch);
  }

  @Override
  public int hashCode() {
    return Objects.hash(agentId, agentString, agentTimezone, agentLocale, agentOsName, agentOsVersion, agentOsArch);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
