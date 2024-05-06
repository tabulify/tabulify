package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.exception.InternalException;
import net.bytle.tower.EraldyModel;
import net.bytle.tower.eraldy.module.organization.model.OrgaRole;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.tower.eraldy.module.organization.model.Organization;
import net.bytle.tower.eraldy.module.realm.model.Realm;
import net.bytle.tower.eraldy.module.realm.model.UserGuid;

import java.util.Objects;

/**
 * A user:
 * * in the Eraldy realm
 * * that belongs / has an organization (Multiple orga is not supported)
 * <p>
 * They may own Realms, App, List.
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgaUser extends User {


  protected Organization organization;
  private OrgaRole organizationRole;

  /**
   * The empty constructor is
   * needed for the construction of the pojo
   * with the Jackson library
   */
  @SuppressWarnings("unused")
  public OrgaUser() {
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
  public void setOrganization(Organization organization) {
    this.organization = organization;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OrgaUser orgaUser = (OrgaUser) o;
    return super.equals(o) &&

      Objects.equals(organization, orgaUser.organization);
  }

  @Override
  public void setRealm(Realm realm) {
    if (realm.getGuid().getLocalId()!=EraldyModel.REALM_LOCAL_ID) {
      throw new RuntimeException("The realm of an orga user should be the Eraldy realm");
    }
    super.setRealm(realm);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), organization);
  }

  @Override
  public String toString() {
    return super.toString();
  }

  public void setOrganizationRole(OrgaRole role) {
    this.organizationRole = role;
  }

  @JsonProperty("organizationRole")
  public OrgaRole getOrganizationRole() {
    return this.organizationRole;
  }

  @Override
  public OrgaUserGuid getGuid() {
    /**
     * Important to serialize to
     */
    return (OrgaUserGuid) super.getGuid();
  }

  @Override
  public void setGuid(UserGuid guid) {
    if (!(guid instanceof OrgaUserGuid)){
      throw new InternalException("The guid should be an orga user guid, not a user guid.");
    }
    super.setGuid(guid);
  }
}
