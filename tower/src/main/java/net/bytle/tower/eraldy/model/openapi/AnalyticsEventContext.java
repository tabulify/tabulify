package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * The event context A dictionary of extra information that provides useful context about a event We follow https://segment.com/docs/connections/spec/common/#context
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsEventContext   {


  protected String timezone;

  protected String locale;

  protected String ip;

  protected String userAgent;

  protected AnalyticsEventChannel channel;

  protected AnalyticsOperatingSystem os;

  protected AnalyticsUrchinTrackingModule utm;

  protected AnalyticsBrowser browser;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public AnalyticsEventContext () {
  }

  /**
  * @return timezone the timezone id (ie Europe/London)
  */
  @JsonProperty("timezone")
  public String getTimezone() {
    return timezone;
  }

  /**
  * @param timezone the timezone id (ie Europe/London)
  */
  @SuppressWarnings("unused")
  public void setTimezone(String timezone) {
    this.timezone = timezone;
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
  * @return ip The ip (the remote client ip)
  */
  @JsonProperty("ip")
  public String getIp() {
    return ip;
  }

  /**
  * @param ip The ip (the remote client ip)
  */
  @SuppressWarnings("unused")
  public void setIp(String ip) {
    this.ip = ip;
  }

  /**
  * @return userAgent The user agent string
  */
  @JsonProperty("userAgent")
  public String getUserAgent() {
    return userAgent;
  }

  /**
  * @param userAgent The user agent string
  */
  @SuppressWarnings("unused")
  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  /**
  * @return channel
  */
  @JsonProperty("channel")
  public AnalyticsEventChannel getChannel() {
    return channel;
  }

  /**
  * @param channel Set channel
  */
  @SuppressWarnings("unused")
  public void setChannel(AnalyticsEventChannel channel) {
    this.channel = channel;
  }

  /**
  * @return os
  */
  @JsonProperty("os")
  public AnalyticsOperatingSystem getOs() {
    return os;
  }

  /**
  * @param os Set os
  */
  @SuppressWarnings("unused")
  public void setOs(AnalyticsOperatingSystem os) {
    this.os = os;
  }

  /**
  * @return utm
  */
  @JsonProperty("utm")
  public AnalyticsUrchinTrackingModule getUtm() {
    return utm;
  }

  /**
  * @param utm Set utm
  */
  @SuppressWarnings("unused")
  public void setUtm(AnalyticsUrchinTrackingModule utm) {
    this.utm = utm;
  }

  /**
  * @return browser
  */
  @JsonProperty("browser")
  public AnalyticsBrowser getBrowser() {
    return browser;
  }

  /**
  * @param browser Set browser
  */
  @SuppressWarnings("unused")
  public void setBrowser(AnalyticsBrowser browser) {
    this.browser = browser;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnalyticsEventContext analyticsEventContext = (AnalyticsEventContext) o;
    return

            Objects.equals(timezone, analyticsEventContext.timezone) && Objects.equals(locale, analyticsEventContext.locale) && Objects.equals(ip, analyticsEventContext.ip) && Objects.equals(userAgent, analyticsEventContext.userAgent) && Objects.equals(channel, analyticsEventContext.channel) && Objects.equals(os, analyticsEventContext.os) && Objects.equals(utm, analyticsEventContext.utm) && Objects.equals(browser, analyticsEventContext.browser);
  }

  @Override
  public int hashCode() {
    return Objects.hash(timezone, locale, ip, userAgent, channel, os, utm, browser);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
