package net.bytle.vertx.analytics.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * The properties of the app that created this event
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsEventApp   {


  protected String appId;

  protected String appHandle;

  protected String appRealmId;

  protected String appRealmHandle;

  protected String appOrganisationId;

  protected String appOrganisationHandle;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public AnalyticsEventApp () {
  }

  /**
  * @return appId The app id that has created this event (never change)
  */
  @JsonProperty("appId")
  public String getAppId() {
    return appId;
  }

  /**
  * @param appId The app id that has created this event (never change)
  */
  @SuppressWarnings("unused")
  public void setAppId(String appId) {
    this.appId = appId;
  }

  /**
  * @return appHandle The app name identifier (human identifier, may change)
  */
  @JsonProperty("appHandle")
  public String getAppHandle() {
    return appHandle;
  }

  /**
  * @param appHandle The app name identifier (human identifier, may change)
  */
  @SuppressWarnings("unused")
  public void setAppHandle(String appHandle) {
    this.appHandle = appHandle;
  }

  /**
  * @return appRealmId The realm of the app If the user is not anonymous, it is the user audience.
  */
  @JsonProperty("appRealmId")
  public String getAppRealmId() {
    return appRealmId;
  }

  /**
  * @param appRealmId The realm of the app If the user is not anonymous, it is the user audience.
  */
  @SuppressWarnings("unused")
  public void setAppRealmId(String appRealmId) {
    this.appRealmId = appRealmId;
  }

  /**
  * @return appRealmHandle The realm human identifier (may change)
  */
  @JsonProperty("appRealmHandle")
  public String getAppRealmHandle() {
    return appRealmHandle;
  }

  /**
  * @param appRealmHandle The realm human identifier (may change)
  */
  @SuppressWarnings("unused")
  public void setAppRealmHandle(String appRealmHandle) {
    this.appRealmHandle = appRealmHandle;
  }

  /**
  * @return appOrganisationId the organisation id (the billing logical unit, known also as the group id)
  */
  @JsonProperty("appOrganisationId")
  public String getAppOrganisationId() {
    return appOrganisationId;
  }

  /**
  * @param appOrganisationId the organisation id (the billing logical unit, known also as the group id)
  */
  @SuppressWarnings("unused")
  public void setAppOrganisationId(String appOrganisationId) {
    this.appOrganisationId = appOrganisationId;
  }

  /**
  * @return appOrganisationHandle the organisation handle (human identifier, may change)
  */
  @JsonProperty("appOrganisationHandle")
  public String getAppOrganisationHandle() {
    return appOrganisationHandle;
  }

  /**
  * @param appOrganisationHandle the organisation handle (human identifier, may change)
  */
  @SuppressWarnings("unused")
  public void setAppOrganisationHandle(String appOrganisationHandle) {
    this.appOrganisationHandle = appOrganisationHandle;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnalyticsEventApp analyticsEventApp = (AnalyticsEventApp) o;
    return

            Objects.equals(appId, analyticsEventApp.appId) && Objects.equals(appHandle, analyticsEventApp.appHandle) && Objects.equals(appRealmId, analyticsEventApp.appRealmId) && Objects.equals(appRealmHandle, analyticsEventApp.appRealmHandle) && Objects.equals(appOrganisationId, analyticsEventApp.appOrganisationId) && Objects.equals(appOrganisationHandle, analyticsEventApp.appOrganisationHandle);
  }

  @Override
  public int hashCode() {
    return Objects.hash(appId, appHandle, appRealmId, appRealmHandle, appOrganisationId, appOrganisationHandle);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
