package net.bytle.tower.eraldy.module.organization.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.model.openapi.OrgaUser;
import net.bytle.tower.eraldy.module.realm.model.Realm;
import net.bytle.type.Handle;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Organization for the users using the Combostrap product (Not from the user of other Realm)
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Organization {


  protected OrgaGuid guid;


  protected Handle handle;

  protected String name;

  protected LocalDateTime creationTime;

  protected LocalDateTime modificationTime;
  private OrgaUser ownerUser;
  private Realm realm;


  public Organization() {

  }



  public static Organization createFromOrgGuid(OrgaGuid orgaGuid) {
    Organization organization = new Organization();
    organization.setGuid(orgaGuid);
    Realm realm = Realm.createFromAnyId(orgaGuid.getRealmId());
    organization.setRealm(realm);
    return organization;
  }

  public static Organization createFromOrgaUserGuid(OrgaUserGuid orgaUserGuid) {
    OrgaGuid orgaGuid = new OrgaGuid();
    orgaGuid.setOrgaId(orgaUserGuid.getOrganizationId());
    orgaGuid.setRealmId(orgaUserGuid.getRealmId());
    return createFromOrgGuid(orgaGuid);
  }

  /**
   * @return guid The string representation of the organization id
   */
  @JsonProperty("guid")
  public OrgaGuid getGuid() {
    return guid;
  }

  /**
   * @param guid The string representation of the organization id
   */
  @SuppressWarnings("unused")
  public void setGuid(OrgaGuid guid) {
    this.guid = guid;
  }

  /**
   * @return handle Organization Handle (a human identifier)
   */
  @JsonProperty("handle")
  public Handle getHandle() {
    return handle;
  }

  /**
   * @param handle Organization Handle (a human identifier)
   */
  @SuppressWarnings("unused")
  public void setHandle(Handle handle) {
    this.handle = handle;
  }

  /**
   * @return name Organization name
   */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
   * @param name Organization name
   */
  @SuppressWarnings("unused")
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return creationTime The creation time of the user in UTC
   */
  @JsonProperty("creationTime")
  public LocalDateTime getCreationTime() {
    return creationTime;
  }

  /**
   * @param creationTime The creation time of the user in UTC
   */
  @SuppressWarnings("unused")
  public void setCreationTime(LocalDateTime creationTime) {
    this.creationTime = creationTime;
  }

  /**
   * @return modificationTime The last modification time of the user in UTC
   */
  @JsonProperty("modificationTime")
  public LocalDateTime getModificationTime() {
    return modificationTime;
  }

  /**
   * @param modificationTime The last modification time of the user in UTC
   */
  @SuppressWarnings("unused")
  public void setModificationTime(LocalDateTime modificationTime) {
    this.modificationTime = modificationTime;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Organization organization = (Organization) o;
    return Objects.equals(guid, organization.guid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(guid);
  }

  @Override
  public String toString() {
    return "guid:" + guid.toString() + ", name: " + this.name;
  }

  public void setOwnerUser(OrgaUser ownerUser) {
    this.ownerUser = ownerUser;
  }

  public OrgaUser getOwnerUser() {
    return ownerUser;
  }

  public void setRealm(Realm realm) {
    this.realm = realm;
  }

  public Realm getRealm() {
    return realm;
  }
}
