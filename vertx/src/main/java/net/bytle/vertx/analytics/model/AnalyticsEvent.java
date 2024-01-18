package net.bytle.vertx.analytics.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The event. They are the things that happen in our product.  We follow a mix of * the &lt;a href&#x3D;\&quot;https://segment.com/docs/connections/spec/common/\&quot;&gt;Segment Spec&lt;/a&gt; * the &lt;a href&#x3D;\&quot;https://segment.com/docs/connections/spec/common/\&quot;&gt;MixPanel Spec&lt;/a&gt;  All properties are on the same level, no hierarchy (as MixPanel do)
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsEvent   {


  protected String id;

  protected String name;

  protected AnalyticsEventSource source;

  protected String appId;

  protected String appRealmId;

  protected String appOrganisationId;

  protected Map<String, Object> properties = new HashMap<>();

  protected String deviceId;

  protected String userId;

  protected String userEmail;

  protected LocalDateTime receptionTime;

  protected LocalDateTime sendingTime;

  protected LocalDateTime creationTime;

  protected String osName;

  protected String osVersion;

  protected String osArch;

  protected URI originUri;

  protected URI referrerUri;

  protected String timezoneId;

  protected String locale;

  protected String remoteIp;

  protected String userAgent;

  protected String utmCampaignId;

  protected String utmCampaignName;

  protected String utmSource;

  protected String utmMedium;

  protected String utmTerm;

  protected String utmContent;

  protected URI utmReferrer;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public AnalyticsEvent () {
  }

  /**
  * @return id A unique identifier for each event.  It allows to delete duplicate.  It's known for: * segment as messageId * mixpanel as $insert_id
  */
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  /**
  * @param id A unique identifier for each event.  It allows to delete duplicate.  It's known for: * segment as messageId * mixpanel as $insert_id
  */
  @SuppressWarnings("unused")
  public void setId(String id) {
    this.id = id;
  }

  /**
  * @return name the event name
  */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
  * @param name the event name
  */
  @SuppressWarnings("unused")
  public void setName(String name) {
    this.name = name;
  }

  /**
  * @return source
  */
  @JsonProperty("source")
  public AnalyticsEventSource getSource() {
    return source;
  }

  /**
  * @param source Set source
  */
  @SuppressWarnings("unused")
  public void setSource(AnalyticsEventSource source) {
    this.source = source;
  }

  /**
  * @return appId The app id that has created this event
  */
  @JsonProperty("appId")
  public String getAppId() {
    return appId;
  }

  /**
  * @param appId The app id that has created this event
  */
  @SuppressWarnings("unused")
  public void setAppId(String appId) {
    this.appId = appId;
  }

  /**
  * @return appRealmId the realm of the app (known also as the audience for a user)
  */
  @JsonProperty("appRealmId")
  public String getAppRealmId() {
    return appRealmId;
  }

  /**
  * @param appRealmId the realm of the app (known also as the audience for a user)
  */
  @SuppressWarnings("unused")
  public void setAppRealmId(String appRealmId) {
    this.appRealmId = appRealmId;
  }

  /**
  * @return appOrganisationId the organization id (the billing logical unit, known also as the group id)
  */
  @JsonProperty("appOrganisationId")
  public String getAppOrganisationId() {
    return appOrganisationId;
  }

  /**
  * @param appOrganisationId the organization id (the billing logical unit, known also as the group id)
  */
  @SuppressWarnings("unused")
  public void setAppOrganisationId(String appOrganisationId) {
    this.appOrganisationId = appOrganisationId;
  }

  /**
  * @return properties The additional event properties
  */
  @JsonProperty("properties")
  public Map<String, Object> getProperties() {
    return properties;
  }

  /**
  * @param properties The additional event properties
  */
  @SuppressWarnings("unused")
  public void setProperties(Map<String, Object> properties) {
    this.properties = properties;
  }

  /**
  * @return deviceId The device id (a uuid4 or a fingerprint for instance)  It's also known as the user anonymous id because this is the user identifier when the user is unknown.  When you send a message, a userId or a deviceId is required in order to identify a user.  We prefer the term device id (mixpanel) over anonymous id (segment) because it's more meaningful. We track the device.  It's created on the client side and stored by the client more permanently (via a cookie for the browser and stored in local storage for durability).  It's not the same as a session id because: * a session id may be regenerated (when the user sign in for instance). * the device id does not have any security feature. You can't login
  */
  @JsonProperty("deviceId")
  public String getDeviceId() {
    return deviceId;
  }

  /**
  * @param deviceId The device id (a uuid4 or a fingerprint for instance)  It's also known as the user anonymous id because this is the user identifier when the user is unknown.  When you send a message, a userId or a deviceId is required in order to identify a user.  We prefer the term device id (mixpanel) over anonymous id (segment) because it's more meaningful. We track the device.  It's created on the client side and stored by the client more permanently (via a cookie for the browser and stored in local storage for durability).  It's not the same as a session id because: * a session id may be regenerated (when the user sign in for instance). * the device id does not have any security feature. You can't login
  */
  @SuppressWarnings("unused")
  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  /**
  * @return userId A unique identifier
  */
  @JsonProperty("userId")
  public String getUserId() {
    return userId;
  }

  /**
  * @param userId A unique identifier
  */
  @SuppressWarnings("unused")
  public void setUserId(String userId) {
    this.userId = userId;
  }

  /**
  * @return userEmail the user email
  */
  @JsonProperty("userEmail")
  public String getUserEmail() {
    return userEmail;
  }

  /**
  * @param userEmail the user email
  */
  @SuppressWarnings("unused")
  public void setUserEmail(String userEmail) {
    this.userEmail = userEmail;
  }

  /**
  * @return receptionTime The timestamp of when a message was received (if created and send by a client)
  */
  @JsonProperty("receptionTime")
  public LocalDateTime getReceptionTime() {
    return receptionTime;
  }

  /**
  * @param receptionTime The timestamp of when a message was received (if created and send by a client)
  */
  @SuppressWarnings("unused")
  public void setReceptionTime(LocalDateTime receptionTime) {
    this.receptionTime = receptionTime;
  }

  /**
  * @return sendingTime The timestamp of when a message was sent to the analytics endpoint api Time: With Ga, you cannot set the time, It uses the notion of queue https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters#qt
  */
  @JsonProperty("sendingTime")
  public LocalDateTime getSendingTime() {
    return sendingTime;
  }

  /**
  * @param sendingTime The timestamp of when a message was sent to the analytics endpoint api Time: With Ga, you cannot set the time, It uses the notion of queue https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters#qt
  */
  @SuppressWarnings("unused")
  public void setSendingTime(LocalDateTime sendingTime) {
    this.sendingTime = sendingTime;
  }

  /**
  * @return creationTime The timestamp when the object was created
  */
  @JsonProperty("creationTime")
  public LocalDateTime getCreationTime() {
    return creationTime;
  }

  /**
  * @param creationTime The timestamp when the object was created
  */
  @SuppressWarnings("unused")
  public void setCreationTime(LocalDateTime creationTime) {
    this.creationTime = creationTime;
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

  /**
  * @return originUri The origin address uri The uri in the address bar in the browser or a iframe uri
  */
  @JsonProperty("originUri")
  public URI getOriginUri() {
    return originUri;
  }

  /**
  * @param originUri The origin address uri The uri in the address bar in the browser or a iframe uri
  */
  @SuppressWarnings("unused")
  public void setOriginUri(URI originUri) {
    this.originUri = originUri;
  }

  /**
  * @return referrerUri The `document.referrer` Note that Google Analytics will use \"utm_referrer\" over \"document.referrer\" if set as document.referrer is only the domain/authority part
  */
  @JsonProperty("referrerUri")
  public URI getReferrerUri() {
    return referrerUri;
  }

  /**
  * @param referrerUri The `document.referrer` Note that Google Analytics will use \"utm_referrer\" over \"document.referrer\" if set as document.referrer is only the domain/authority part
  */
  @SuppressWarnings("unused")
  public void setReferrerUri(URI referrerUri) {
    this.referrerUri = referrerUri;
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
  * @return remoteIp The ip (the remote client ip)
  */
  @JsonProperty("remoteIp")
  public String getRemoteIp() {
    return remoteIp;
  }

  /**
  * @param remoteIp The ip (the remote client ip)
  */
  @SuppressWarnings("unused")
  public void setRemoteIp(String remoteIp) {
    this.remoteIp = remoteIp;
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
  * @return utmCampaignId The Campaign ID is used to identify a specific ad campaign or promotion Known as utm_id, this is a required key for GA4 data import.
  */
  @JsonProperty("utmCampaignId")
  public String getUtmCampaignId() {
    return utmCampaignId;
  }

  /**
  * @param utmCampaignId The Campaign ID is used to identify a specific ad campaign or promotion Known as utm_id, this is a required key for GA4 data import.
  */
  @SuppressWarnings("unused")
  public void setUtmCampaignId(String utmCampaignId) {
    this.utmCampaignId = utmCampaignId;
  }

  /**
  * @return utmCampaignName Product, slogan, promo code
  */
  @JsonProperty("utmCampaignName")
  public String getUtmCampaignName() {
    return utmCampaignName;
  }

  /**
  * @param utmCampaignName Product, slogan, promo code
  */
  @SuppressWarnings("unused")
  public void setUtmCampaignName(String utmCampaignName) {
    this.utmCampaignName = utmCampaignName;
  }

  /**
  * @return utmSource Referrer. Identifies which site sent the traffic, and is a required parameter Example: the referrer (e.g. google, newsletter)
  */
  @JsonProperty("utmSource")
  public String getUtmSource() {
    return utmSource;
  }

  /**
  * @param utmSource Referrer. Identifies which site sent the traffic, and is a required parameter Example: the referrer (e.g. google, newsletter)
  */
  @SuppressWarnings("unused")
  public void setUtmSource(String utmSource) {
    this.utmSource = utmSource;
  }

  /**
  * @return utmMedium Marketing medium (e.g. cpc, banner, email) Identifies what type of link was used, such as cost per click or email.
  */
  @JsonProperty("utmMedium")
  public String getUtmMedium() {
    return utmMedium;
  }

  /**
  * @param utmMedium Marketing medium (e.g. cpc, banner, email) Identifies what type of link was used, such as cost per click or email.
  */
  @SuppressWarnings("unused")
  public void setUtmMedium(String utmMedium) {
    this.utmMedium = utmMedium;
  }

  /**
  * @return utmTerm Identifies search terms, the paid keywords
  */
  @JsonProperty("utmTerm")
  public String getUtmTerm() {
    return utmTerm;
  }

  /**
  * @param utmTerm Identifies search terms, the paid keywords
  */
  @SuppressWarnings("unused")
  public void setUtmTerm(String utmTerm) {
    this.utmTerm = utmTerm;
  }

  /**
  * @return utmContent Identifies what specifically was clicked to bring the user to the site, such as a banner ad or a text link. It is often used for A/B testing and content-targeted ads
  */
  @JsonProperty("utmContent")
  public String getUtmContent() {
    return utmContent;
  }

  /**
  * @param utmContent Identifies what specifically was clicked to bring the user to the site, such as a banner ad or a text link. It is often used for A/B testing and content-targeted ads
  */
  @SuppressWarnings("unused")
  public void setUtmContent(String utmContent) {
    this.utmContent = utmContent;
  }

  /**
  * @return utmReferrer Identifies the referrer URL as it's not passed by default by browser HTTP request and therefore not available in the `document.referrer` attribute Google Analytics will use \"utm_referrer\" over \"document.referrer\" set in AnalyticsBrowser utm_source is also a referer but in a named format
  */
  @JsonProperty("utmReferrer")
  public URI getUtmReferrer() {
    return utmReferrer;
  }

  /**
  * @param utmReferrer Identifies the referrer URL as it's not passed by default by browser HTTP request and therefore not available in the `document.referrer` attribute Google Analytics will use \"utm_referrer\" over \"document.referrer\" set in AnalyticsBrowser utm_source is also a referer but in a named format
  */
  @SuppressWarnings("unused")
  public void setUtmReferrer(URI utmReferrer) {
    this.utmReferrer = utmReferrer;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnalyticsEvent analyticsEvent = (AnalyticsEvent) o;
    return

            Objects.equals(id, analyticsEvent.id) && Objects.equals(name, analyticsEvent.name) && Objects.equals(source, analyticsEvent.source) && Objects.equals(appId, analyticsEvent.appId) && Objects.equals(appRealmId, analyticsEvent.appRealmId) && Objects.equals(appOrganisationId, analyticsEvent.appOrganisationId) && Objects.equals(properties, analyticsEvent.properties) && Objects.equals(deviceId, analyticsEvent.deviceId) && Objects.equals(userId, analyticsEvent.userId) && Objects.equals(userEmail, analyticsEvent.userEmail) && Objects.equals(receptionTime, analyticsEvent.receptionTime) && Objects.equals(sendingTime, analyticsEvent.sendingTime) && Objects.equals(creationTime, analyticsEvent.creationTime) && Objects.equals(osName, analyticsEvent.osName) && Objects.equals(osVersion, analyticsEvent.osVersion) && Objects.equals(osArch, analyticsEvent.osArch) && Objects.equals(originUri, analyticsEvent.originUri) && Objects.equals(referrerUri, analyticsEvent.referrerUri) && Objects.equals(timezoneId, analyticsEvent.timezoneId) && Objects.equals(locale, analyticsEvent.locale) && Objects.equals(remoteIp, analyticsEvent.remoteIp) && Objects.equals(userAgent, analyticsEvent.userAgent) && Objects.equals(utmCampaignId, analyticsEvent.utmCampaignId) && Objects.equals(utmCampaignName, analyticsEvent.utmCampaignName) && Objects.equals(utmSource, analyticsEvent.utmSource) && Objects.equals(utmMedium, analyticsEvent.utmMedium) && Objects.equals(utmTerm, analyticsEvent.utmTerm) && Objects.equals(utmContent, analyticsEvent.utmContent) && Objects.equals(utmReferrer, analyticsEvent.utmReferrer);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, source, appId, appRealmId, appOrganisationId, properties, deviceId, userId, userEmail, receptionTime, sendingTime, creationTime, osName, osVersion, osArch, originUri, referrerUri, timezoneId, locale, remoteIp, userAgent, utmCampaignId, utmCampaignName, utmSource, utmMedium, utmTerm, utmContent, utmReferrer);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
