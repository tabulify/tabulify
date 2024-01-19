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

  protected String appRealmId;

  protected String appOrganisationId;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public AnalyticsEventApp () {
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

            Objects.equals(appId, analyticsEventApp.appId) && Objects.equals(appRealmId, analyticsEventApp.appRealmId) && Objects.equals(appOrganisationId, analyticsEventApp.appOrganisationId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(appId, appRealmId, appOrganisationId);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
