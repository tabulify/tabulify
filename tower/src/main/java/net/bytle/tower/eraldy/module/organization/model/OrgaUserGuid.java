package net.bytle.tower.eraldy.module.organization.model;

import net.bytle.exception.InternalException;
import net.bytle.tower.EraldyModel;
import net.bytle.tower.eraldy.module.user.model.UserGuid;

import java.util.Objects;

/**
 * An orga user guid is the Eraldy Realm id 1
 * and a local id
 */
public class OrgaUserGuid extends UserGuid {


  public OrgaUserGuid() {
    super();
  }


  @Override
  public void setLocalId(Long localId) {

    if (!Objects.equals(localId,EraldyModel.REALM_LOCAL_ID)) {
      throw new InternalException("An orga user should have the Eraldy Realm");
    }
    super.setLocalId(localId);

  }

  @Override
  public Long getRealmId() {

    return EraldyModel.REALM_LOCAL_ID;

  }


}
