package net.bytle.tower.eraldy.model.openapi;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;

/**
 * App creation, modification If:   * the guid is defined, it will be used to update the existing app.   * not, the uri is used instead. The user (guid or email) is mandatory (the public face of the app) The realm (guid or handle) is also mandatory 
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppPostBody   {


  protected String appGuid;

  protected String appUri;

  protected String realmIdentifier;

  protected String userGuid;

  protected String userEmail;

  protected String appName;

  protected String appSlogan;

  protected URI appHome;

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
  * @return appUri An unique uri name of the app
  */
  @JsonProperty("appUri")
  public String getAppUri() {
    return appUri;
  }

  /**
  * @param appUri An unique uri name of the app
  */
  @SuppressWarnings("unused")
  public void setAppUri(String appUri) {
    this.appUri = appUri;
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
  * @return userGuid The guid of the public app user
  */
  @JsonProperty("userGuid")
  public String getUserGuid() {
    return userGuid;
  }

  /**
  * @param userGuid The guid of the public app user
  */
  @SuppressWarnings("unused")
  public void setUserGuid(String userGuid) {
    this.userGuid = userGuid;
  }

  /**
  * @return userEmail The email of the public app user
  */
  @JsonProperty("userEmail")
  public String getUserEmail() {
    return userEmail;
  }

  /**
  * @param userEmail The email of the public app user
  */
  @SuppressWarnings("unused")
  public void setUserEmail(String userEmail) {
    this.userEmail = userEmail;
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
  * @return appHome The url of the app
  */
  @JsonProperty("appHome")
  public URI getAppHome() {
    return appHome;
  }

  /**
  * @param appHome The url of the app
  */
  @SuppressWarnings("unused")
  public void setAppHome(URI appHome) {
    this.appHome = appHome;
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
            
            Objects.equals(appGuid, appPostBody.appGuid) && Objects.equals(appUri, appPostBody.appUri) && Objects.equals(realmIdentifier, appPostBody.realmIdentifier) && Objects.equals(userGuid, appPostBody.userGuid) && Objects.equals(userEmail, appPostBody.userEmail) && Objects.equals(appName, appPostBody.appName) && Objects.equals(appSlogan, appPostBody.appSlogan) && Objects.equals(appHome, appPostBody.appHome) && Objects.equals(appLogo, appPostBody.appLogo) && Objects.equals(appPrimaryColor, appPostBody.appPrimaryColor) && Objects.equals(appTerms, appPostBody.appTerms);
  }

  @Override
  public int hashCode() { 
    return Objects.hash(appGuid, appUri, realmIdentifier, userGuid, userEmail, appName, appSlogan, appHome, appLogo, appPrimaryColor, appTerms);
  }

  @Override 
  public String toString() {
    return super.toString();
  }

}
