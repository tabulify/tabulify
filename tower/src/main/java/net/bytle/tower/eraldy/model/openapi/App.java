package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.Objects;

/**
 * An app is the container for branding elements (such as logo, color)
 **/
@com.fasterxml.jackson.annotation.JsonIdentityInfo( generator = com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator.class, property = "guid", scope = Realm.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class App   {


  protected Long localId;

  protected String guid;

  protected URI uri;

  protected String clientId;

  protected String name;

  protected URI home;

  protected String slogan;

  protected URI logo;

  protected String primaryColor;

  protected User user;

  protected Realm realm;

  protected URI terms;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public App () {
  }

  /**
  * @return localId The app identifier in the realm (without the realm, the id may have duplicate)
  */
  @JsonProperty("localId")
  public Long getLocalId() {
    return localId;
  }

  /**
  * @param localId The app identifier in the realm (without the realm, the id may have duplicate)
  */
  @SuppressWarnings("unused")
  public void setLocalId(Long localId) {
    this.localId = localId;
  }

  /**
  * @return guid The global publisher id (realm id + publisher id)
  */
  @JsonProperty("guid")
  public String getGuid() {
    return guid;
  }

  /**
  * @param guid The global publisher id (realm id + publisher id)
  */
  @SuppressWarnings("unused")
  public void setGuid(String guid) {
    this.guid = guid;
  }

  /**
  * @return uri The uri of the app (unique for all apps on the realm, this is equivalent to the authentication scope)
  */
  @JsonProperty("uri")
  public URI getUri() {
    return uri;
  }

  /**
  * @param uri The uri of the app (unique for all apps on the realm, this is equivalent to the authentication scope)
  */
  @SuppressWarnings("unused")
  public void setUri(URI uri) {
    this.uri = uri;
  }

  /**
  * @return clientId The client id used to connect
  */
  @JsonProperty("clientId")
  public String getClientId() {
    return clientId;
  }

  /**
  * @param clientId The client id used to connect
  */
  @SuppressWarnings("unused")
  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  /**
  * @return name The name of the app
  */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
  * @param name The name of the app
  */
  @SuppressWarnings("unused")
  public void setName(String name) {
    this.name = name;
  }

  /**
  * @return home The home url
  */
  @JsonProperty("home")
  public URI getHome() {
    return home;
  }

  /**
  * @param home The home url
  */
  @SuppressWarnings("unused")
  public void setHome(URI home) {
    this.home = home;
  }

  /**
  * @return slogan The slogan of the app
  */
  @JsonProperty("slogan")
  public String getSlogan() {
    return slogan;
  }

  /**
  * @param slogan The slogan of the app
  */
  @SuppressWarnings("unused")
  public void setSlogan(String slogan) {
    this.slogan = slogan;
  }

  /**
  * @return logo The uri of the app logo
  */
  @JsonProperty("logo")
  public URI getLogo() {
    return logo;
  }

  /**
  * @param logo The uri of the app logo
  */
  @SuppressWarnings("unused")
  public void setLogo(URI logo) {
    this.logo = logo;
  }

  /**
  * @return primaryColor The css primary color of the theme (rgb in hexadecimal)
  */
  @JsonProperty("primaryColor")
  public String getPrimaryColor() {
    return primaryColor;
  }

  /**
  * @param primaryColor The css primary color of the theme (rgb in hexadecimal)
  */
  @SuppressWarnings("unused")
  public void setPrimaryColor(String primaryColor) {
    this.primaryColor = primaryColor;
  }

  /**
  * @return user
  */
  @JsonProperty("user")
  public User getUser() {
    return user;
  }

  /**
  * @param user Set user
  */
  @SuppressWarnings("unused")
  public void setUser(User user) {
    this.user = user;
  }

  /**
  * @return realm
  */
  @JsonProperty("realm")
  public Realm getRealm() {
    return realm;
  }

  /**
  * @param realm Set realm
  */
  @SuppressWarnings("unused")
  public void setRealm(Realm realm) {
    this.realm = realm;
  }

  /**
  * @return terms The location of the terms and conditions document
  */
  @JsonProperty("terms")
  public URI getTerms() {
    return terms;
  }

  /**
  * @param terms The location of the terms and conditions document
  */
  @SuppressWarnings("unused")
  public void setTerms(URI terms) {
    this.terms = terms;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    App app = (App) o;
    return Objects.equals(localId, app.localId) &&
        Objects.equals(guid, app.guid) &&
        Objects.equals(uri, app.uri) &&
        Objects.equals(clientId, app.clientId) &&
        Objects.equals(name, app.name) &&
        Objects.equals(home, app.home) &&
        Objects.equals(slogan, app.slogan) &&
        Objects.equals(logo, app.logo) &&
        Objects.equals(primaryColor, app.primaryColor) &&
        Objects.equals(user, app.user) &&
        Objects.equals(realm, app.realm) &&
        Objects.equals(terms, app.terms);
  }

  @Override
  public int hashCode() {
    return Objects.hash(guid, uri);
  }

  @Override
  public String toString() {
    return "class App {\n" +

    "    guid: " + toIndentedString(guid) + "\n" +

    "    uri: " + toIndentedString(uri) + "\n" +
    "}";
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
