package net.bytle.tower.eraldy.module.organization.model;

import net.bytle.tower.EraldyModel;

/**
 * A orga guid is the realm id 1
 * and a local id
 */
public class OrgaUserGuid {


  Long localId;

  public OrgaUserGuid() {
  }

  public void setLocalId(long localId) {
    this.localId = localId;
  }

  public Long getLocalId() {
    return this.localId;
  }

  public Long getRealmId() {
    return EraldyModel.REALM_LOCAL_ID;
  }

}
