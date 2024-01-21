package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.Objects;

/**
 * App creation, modification If:   * the guid is defined, it will be used to update the existing app.   * not, the uri is used instead. The user (guid or email) is mandatory (the public face of the app) The realm (guid or handle) is also mandatory
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppPostBody   {


  protected String appGuid;

  protected String appHandle;

  protected URI appUri;

  protected URI appHome;

  protected String realmIdentifier;

  protected String userIdentifier;

  protected String appName;

  protected String appSlogan;

  protected URI appLogo;

  protected String appPrimaryColor;

  protected URI appTerms;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public AppPostBody () {
  }

  /**
  * @return appGuid The guid of an existing app (to update the uri if necessary)
  */
  @JsonProperty("appGuid")
  public String getAppGuid() {
    return appGuid;
  }

  /**
  * @param appGuid The guid of an existing app (to update the uri if necessary)
  */
  @SuppressWarnings("unused")
  public void setAppGuid(String appGuid) {
    this.appGuid = appGuid;
  }

  /**
  * @return appHandle An unique name of the app
  */
  @JsonProperty("appHandle")
  public String getAppHandle() {
    return appHandle;
  }

  /**
  * @param appHandle An unique name of the app
  */
  @SuppressWarnings("unused")
  public void setAppHandle(String appHandle) {
    this.appHandle = appHandle;
  }

  /**
  * @return appUri The Authentication URI of the App (where the app can be found and where the auth redirection should occurs)
  */
  @JsonProperty("appUri")
  public URI getAppUri() {
    return appUri;
  }

  /**
  * @param appUri The Authentication URI of the App (where the app can be found and where the auth redirection should occurs)
  */
  @SuppressWarnings("unused")
  public void setAppUri(URI appUri) {
    this.appUri = appUri;
  }

  /**
  * @return appHome The Home URL of the App This is an element of branding
  */
  @JsonProperty("appHome")
  public URI getAppHome() {
    return appHome;
  }

  /**
  * @param appHome The Home URL of the App This is an element of branding
  */
  @SuppressWarnings("unused")
  public void setAppHome(URI appHome) {
    this.appHome = appHome;
  }

  /**
  * @return realmIdentifier the realm identifier (Guid or handle)
  */
  @JsonProperty("realmIdentifier")
  public String getRealmIdentifier() {
    return realmIdentifier;
  }

  /**
  * @param realmIdentifier the realm identifier (Guid or handle)
  */
  @SuppressWarnings("unused")
  public void setRealmIdentifier(String realmIdentifier) {
    this.realmIdentifier = realmIdentifier;
  }

  /**
  * @return userIdentifier The user identifier of the public app user (a guid or an email)
  */
  @JsonProperty("userIdentifier")
  public String getUserIdentifier() {
    return userIdentifier;
  }

  /**
  * @param userIdentifier The user identifier of the public app user (a guid or an email)
  */
  @SuppressWarnings("unused")
  public void setUserIdentifier(String userIdentifier) {
    this.userIdentifier = userIdentifier;
  }

  /**
  * @return appName A shortname
  */
  @JsonProperty("appName")
  public String getAppName() {
    return appName;
  }

  /**
  * @param appName A shortname
  */
  @SuppressWarnings("unused")
  public void setAppName(String appName) {
    this.appName = appName;
  }

  /**
  * @return appSlogan The app slogan
  */
  @JsonProperty("appSlogan")
  public String getAppSlogan() {
    return appSlogan;
  }

  /**
  * @param appSlogan The app slogan
  */
  @SuppressWarnings("unused")
  public void setAppSlogan(String appSlogan) {
    this.appSlogan = appSlogan;
  }

  /**
  * @return appLogo The uri of a logo
  */
  @JsonProperty("appLogo")
  public URI getAppLogo() {
    return appLogo;
  }

  /**
  * @param appLogo The uri of a logo
  */
  @SuppressWarnings("unused")
  public void setAppLogo(URI appLogo) {
    this.appLogo = appLogo;
  }

  /**
  * @return appPrimaryColor The css primary color of the theme (rgb in hexadecimal)
  */
  @JsonProperty("appPrimaryColor")
  public String getAppPrimaryColor() {
    return appPrimaryColor;
  }

  /**
  * @param appPrimaryColor The css primary color of the theme (rgb in hexadecimal)
  */
  @SuppressWarnings("unused")
  public void setAppPrimaryColor(String appPrimaryColor) {
    this.appPrimaryColor = appPrimaryColor;
  }

  /**
  * @return appTerms The url of the terms and conditions document
  */
  @JsonProperty("appTerms")
  public URI getAppTerms() {
    return appTerms;
  }

  /**
  * @param appTerms The url of the terms and conditions document
  */
  @SuppressWarnings("unused")
  public void setAppTerms(URI appTerms) {
    this.appTerms = appTerms;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppPostBody appPostBody = (AppPostBody) o;
    return

            Objects.equals(appGuid, appPostBody.appGuid) && Objects.equals(appHandle, appPostBody.appHandle) && Objects.equals(appUri, appPostBody.appUri) && Objects.equals(appHome, appPostBody.appHome) && Objects.equals(realmIdentifier, appPostBody.realmIdentifier) && Objects.equals(userIdentifier, appPostBody.userIdentifier) && Objects.equals(appName, appPostBody.appName) && Objects.equals(appSlogan, appPostBody.appSlogan) && Objects.equals(appLogo, appPostBody.appLogo) && Objects.equals(appPrimaryColor, appPostBody.appPrimaryColor) && Objects.equals(appTerms, appPostBody.appTerms);
  }

  @Override
  public int hashCode() {
    return Objects.hash(appGuid, appHandle, appUri, appHome, realmIdentifier, userIdentifier, appName, appSlogan, appLogo, appPrimaryColor, appTerms);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
