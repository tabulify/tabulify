package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * A realm.  It identifies a protection space where the user and their credentials are stored.  * In a marketing level, it represents a brand. When people log in, they see the realm logo. * In a web/dns level, it would be a domain name (and apps would be subdomain) * In a security level, this is the authentication realm. * In an infrastructure level, this is called a tenant.
 **/
@com.fasterxml.jackson.annotation.JsonIdentityInfo( generator = com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator.class, property = "guid", scope = Realm.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Realm   {


  protected Long localId;

  protected String guid;

  protected String name;

  protected Organization organization;

  protected User ownerUser;

  protected String handle;

  protected App defaultApp;

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
  * @return name A shor description of the realm
  */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
  * @param name A shor description of the realm
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
  * @return handle The handle is a name unique identifier. It's used as:   - basic authentication: \"WWW-Authenticate: Basic realm=\"WallyWorld\"   - database schema, dns name
  */
  @JsonProperty("handle")
  public String getHandle() {
    return handle;
  }

  /**
  * @param handle The handle is a name unique identifier. It's used as:   - basic authentication: \"WWW-Authenticate: Basic realm=\"WallyWorld\"   - database schema, dns name
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
    return
            Objects.equals(guid, realm.guid) && Objects.equals(handle, realm.handle);

  }

  @Override
  public int hashCode() {
    return Objects.hash(guid, handle);
  }

  @Override
  public String toString() {
    return guid + ", " + handle;
  }

}
