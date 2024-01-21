package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * A user in the organization They are the users of the Eraldy realm that are part of an organization. They may own Realms, App, List.
 **/
@com.fasterxml.jackson.annotation.JsonIdentityInfo( generator = com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator.class, property = "guid", scope = User.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganizationUser extends User  {


  protected Organization organization;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public OrganizationUser () {
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
    OrganizationUser organizationUser = (OrganizationUser) o;
    return super.equals(o) &&

            Objects.equals(organization, organizationUser.organization);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), organization);
  }

  @Override
  public String toString() {
    return super.toString() + super.toString();
  }

}
