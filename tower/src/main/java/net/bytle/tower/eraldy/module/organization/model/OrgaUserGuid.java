package net.bytle.tower.eraldy.module.organization.model;

import net.bytle.tower.EraldyModel;
import net.bytle.tower.eraldy.module.user.model.UserGuid;

/**
 * An orga user guid is:
 * * a user guid where the realm is the eraldy realm 1
 * * and an organization
 * <p>
 * At first, we were extending the UserGuid, but it comes with multiple problem
 * when serializing as the serializer may choose the UserGuid serializer
 * and not the OrgaUserGuid
 * They are 2 differents id
 */
public class OrgaUserGuid {


  private long organizationId;
  private long localId;

  public OrgaUserGuid() {
    super();
  }


  public void setOrganizationId(long organizationId) {

    this.organizationId = organizationId;

  }

  public void setLocalId(long localId) {

    this.localId = localId;

  }

  public long getRealmId() {

    return EraldyModel.REALM_LOCAL_ID;

  }

  public long getOrganizationId() {

    return this.organizationId;

  }


  public UserGuid toUserGuid() {
    UserGuid userGuid = new UserGuid();
    userGuid.setRealmId(EraldyModel.REALM_LOCAL_ID);
    userGuid.setLocalId(this.localId);
    return userGuid;
  }

  public long getLocalId() {
    return this.localId;
  }

}
