package net.bytle.tower.eraldy.module.organization.inputs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.type.Handle;

/**
 * Organization for the users using the Combostrap product (Not from the user of other Realm)
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganizationInputProps {


  protected Handle handle;

  protected String name;

  protected OrgaUserGuid ownerUserGuid;

  public OrgaUserGuid getOwnerUserGuid() {
    return ownerUserGuid;
  }

  public void setOwnerUserGuid(OrgaUserGuid ownerUserGuid) {
    this.ownerUserGuid = ownerUserGuid;
  }

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public OrganizationInputProps() {
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

}
