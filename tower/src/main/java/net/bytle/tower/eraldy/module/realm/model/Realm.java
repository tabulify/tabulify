package net.bytle.tower.eraldy.module.realm.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.model.openapi.OrgaUser;
import net.bytle.tower.eraldy.module.organization.model.Organization;
import net.bytle.type.Handle;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A realm.
 * It identifies a protection space where the user and their credentials are stored.
 * * In a marketing level, it represents a brand. When people log in, they may see the realm logo next to the app logo.
 * * In a web/dns level, it would be a domain name (and apps would be subdomain)
 * * In a security level, this is the authentication realm.
 * * In an infrastructure level, this is called a tenant.
 * <p>
 * For a security level, note that the scope of the credentials are
 * <a href="https://httpwg.org/specs/rfc7617.html#reusing.credentials">URL based</a>
 *
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Realm {


  protected RealmGuid guid;

  protected String name;

  protected Organization organization;

  protected OrgaUser ownerUser;

  protected Handle handle;

  protected Long userCount;

  protected Integer appCount;

  protected Integer listCount;

  protected Integer listUserInCount;
  private LocalDateTime creationTime;
  private LocalDateTime modificationTime;


  public Realm() {
  }

  public static Realm createFromAnyId(long localId) {
    RealmGuid realmGuid = new RealmGuid(localId);
    return createFromAnyId(realmGuid);
  }

  public void setGuid(RealmGuid realmGuid) {
    this.guid = realmGuid;
  }

  public static Realm createFromAnyId(RealmGuid realmGuid) {
    Realm realm = new Realm();
    realm.setGuid(realmGuid);
    return realm;
  }


  /**
   * @return guid The public id (derived from the database id)
   */
  @JsonProperty("guid")
  public RealmGuid getGuid() {
    return guid;
  }


  /**
   * @return name A short description of the realm
   */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
   * @param name A short description of the realm
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
  public OrgaUser getOwnerUser() {
    return ownerUser;
  }

  /**
   * @param ownerUser Set ownerUser
   */
  @SuppressWarnings("unused")
  public void setOwnerUser(OrgaUser ownerUser) {
    this.ownerUser = ownerUser;
  }

  /**
   * @return handle The handle is a name unique identifier. It's used as:   - basic authentication: \"WWW-Authenticate: Basic realm=\"WallyWorld\"   - database schema, dns name
   */
  @JsonProperty("handle")
  public Handle getHandle() {
    return handle;
  }

  /**
   * @param handle The handle is a name unique identifier. It's used as:   - basic authentication: \"WWW-Authenticate: Basic realm=\"WallyWorld\"   - database schema, dns name
   */
  @SuppressWarnings("unused")
  public void setHandle(Handle handle) {
    this.handle = handle;
  }

  /**
   * @return userCount The number of users for the realm
   */
  @JsonProperty("userCount")
  public Long getUserCount() {
    return userCount;
  }

  /**
   * @param userCount The number of users for the realm
   */
  @SuppressWarnings("unused")
  public void setUserCount(Long userCount) {
    this.userCount = userCount;
  }

  /**
   * @return appCount The number of apps for the realm
   */
  @JsonProperty("appCount")
  public Integer getAppCount() {
    return appCount;
  }

  /**
   * @param appCount The number of apps for the realm
   */
  @SuppressWarnings("unused")
  public void setAppCount(Integer appCount) {
    this.appCount = appCount;
  }

  /**
   * @return listCount The number of lists for the realm
   */
  @JsonProperty("listCount")
  public Integer getListCount() {
    return listCount;
  }

  /**
   * @param listCount The number of lists for the realm
   */
  @SuppressWarnings("unused")
  public void setListCount(Integer listCount) {
    this.listCount = listCount;
  }

  /**
   * @return listUserInCount The number of active user on the list
   */
  @JsonProperty("listUserInCount")
  public Integer getListUserInCount() {
    return listUserInCount;
  }

  /**
   * @param listUserInCount The number of active user on the list
   */
  @SuppressWarnings("unused")
  public void setUserInCount(Integer listUserInCount) {
    this.listUserInCount = listUserInCount;
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


  public void setCreationTime(LocalDateTime localDateTime) {
    this.creationTime = localDateTime;
  }

  @JsonProperty("creationTime")
  public LocalDateTime getCreationTime() {
    return creationTime;
  }
  @JsonProperty("modificationTime")
  public LocalDateTime getModificationTime() {
    return modificationTime;
  }

  public void setModificationTime(LocalDateTime modificationTime) {
    this.modificationTime = modificationTime;
  }
}
