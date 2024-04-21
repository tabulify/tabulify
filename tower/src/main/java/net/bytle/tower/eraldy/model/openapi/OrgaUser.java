package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.module.organization.model.OrgaRole;

import java.util.Objects;

/**
 * A user in the Eraldy realm that may be part of an organization
 * * An orga user may have no Organiation.
 * * An orga user is an eraldy user
 * <p>
 * They are the users of the Eraldy realm that are part of an organization.
 * They may own Realms, App, List.
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgaUser extends User  {


  protected Organization organization;
  private OrgaRole orgaRole;

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
  @SuppressWarnings("unused")
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
  public int hashCode() {
    return Objects.hash(super.hashCode(), organization);
  }

  @Override
  public String toString() {
    return super.toString() + super.toString();
  }

  public void setOrgaRole(OrgaRole role) {
    this.orgaRole = role;
  }

  public OrgaRole getOrgaRole() {
    return this.orgaRole;
  }
}
