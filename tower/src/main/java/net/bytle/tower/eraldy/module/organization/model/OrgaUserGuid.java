package net.bytle.tower.eraldy.module.organization.model;

import net.bytle.exception.InternalException;
import net.bytle.tower.EraldyModel;
import net.bytle.tower.eraldy.module.user.model.UserGuid;

import java.util.Objects;

/**
 * An orga user guid is:
 * * a user guid where the realm is the eraldy realm 1
 * * and an organization
 */
public class OrgaUserGuid extends UserGuid {


  private Long organizationId;

  public OrgaUserGuid() {
    super();
  }


  @Override
  public void setRealmId(Long localId) {

    if (!Objects.equals(localId, EraldyModel.REALM_LOCAL_ID)) {
      throw new InternalException("An orga user should have the Eraldy Realm");
    }
    super.setRealmId(localId);

  }

  public void setOrganizationId(Long organizationId) {

    this.organizationId = organizationId;

  }

  @Override
  public Long getRealmId() {

    return EraldyModel.REALM_LOCAL_ID;

  }

  public Long getOrganizationId() {

    return this.organizationId;

  }


}
