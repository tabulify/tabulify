package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * A realm.  It identifies a protection space where the user and their credentials are stored  * In a marketing level, it represent a brand. When people log in, they see the realm logo. * In a web/dns level, it would be the apex domain (and apps would be the DNS entry) * In a security level, this is the authentication realm
 **/
@com.fasterxml.jackson.annotation.JsonIdentityInfo( generator = com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator.class, property = "guid", scope = Realm.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Realm   {

  private Long localId;
  protected String guid;
  private String name;
  private Organization organization;
  private User ownerUser;
  private String handle;
  private App defaultApp;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public Realm () {
  }

  /**
  * @return localId The realm id in the database
  */
  @JsonProperty("localId")
  public Long getLocalId() {
    return localId;
  }

  /**
  * @param localId The realm id in the database
  */
  @SuppressWarnings("unused")
  public void setLocalId(Long localId) {
    this.localId = localId;
  }

  /**
  * @return guid The public id (derived from the database id)
  */
  @JsonProperty("guid")
  public String getGuid() {
    return guid;
  }

  /**
  * @param guid The public id (derived from the database id)
  */
  @SuppressWarnings("unused")
  public void setGuid(String guid) {
    this.guid = guid;
  }

  /**
  * @return name The name of the realm
  */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
  * @param name The name of the realm
  */
  @SuppressWarnings("unused")
  public void setName(String name) {
    this.name = name;
  }

  /**
  * @return organization
  */
  @JsonProperty("organization")
  public Organization getOrganization() {
    return organization;
  }

  /**
  * @param organization Set organization
  */
  @SuppressWarnings("unused")
  public void setOrganization(Organization organization) {
    this.organization = organization;
  }

  /**
  * @return ownerUser
  */
  @JsonProperty("ownerUser")
  public User getOwnerUser() {
    return ownerUser;
  }

  /**
  * @param ownerUser Set ownerUser
  */
  @SuppressWarnings("unused")
  public void setOwnerUser(User ownerUser) {
    this.ownerUser = ownerUser;
  }

  /**
  * @return handle A handle, the name used as:   - basic authentication: \"WWW-Authenticate: Basic realm=\"WallyWorld\"   - database schema
  */
  @JsonProperty("handle")
  public String getHandle() {
    return handle;
  }

  /**
  * @param handle A handle, the name used as:   - basic authentication: \"WWW-Authenticate: Basic realm=\"WallyWorld\"   - database schema
  */
  @SuppressWarnings("unused")
  public void setHandle(String handle) {
    this.handle = handle;
  }

  /**
  * @return defaultApp
  */
  @JsonProperty("defaultApp")
  public App getDefaultApp() {
    return defaultApp;
  }

  /**
  * @param defaultApp Set defaultApp
  */
  @SuppressWarnings("unused")
  public void setDefaultApp(App defaultApp) {
    this.defaultApp = defaultApp;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Realm realm = (Realm) o;
    return Objects.equals(localId, realm.localId) &&
        Objects.equals(guid, realm.guid) &&
        Objects.equals(name, realm.name) &&
        Objects.equals(organization, realm.organization) &&
        Objects.equals(ownerUser, realm.ownerUser) &&
        Objects.equals(handle, realm.handle) &&
        Objects.equals(defaultApp, realm.defaultApp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(localId, guid, name, organization, ownerUser, handle, defaultApp);
  }

  @Override
  public String toString() {
    return "class Realm {\n" +
    "    guid: " + toIndentedString(guid) + "\n" +
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
