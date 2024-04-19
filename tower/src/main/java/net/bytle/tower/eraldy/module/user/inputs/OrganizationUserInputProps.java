package net.bytle.tower.eraldy.module.user.inputs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.model.openapi.Organization;

/**
 * A user in the organization They are the users of the Eraldy realm that are part of an organization. They may own Realms, App, List.
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganizationUserInputProps  {


  protected Organization organization;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public OrganizationUserInputProps() {
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



}
