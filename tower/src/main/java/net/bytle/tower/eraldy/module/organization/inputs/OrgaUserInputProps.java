package net.bytle.tower.eraldy.module.organization.inputs;

import com.fasterxml.jackson.annotation.JsonInclude;
import net.bytle.tower.eraldy.module.organization.model.OrgaRole;

/**
 * Role inside the organization
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgaUserInputProps {


  protected OrgaRole orgaRole;


  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public OrgaUserInputProps() {
  }


  public void setRole(OrgaRole orgaRole) {
    this.orgaRole = orgaRole;
  }

  public OrgaRole getRole() {
    return this.orgaRole;
  }
}
