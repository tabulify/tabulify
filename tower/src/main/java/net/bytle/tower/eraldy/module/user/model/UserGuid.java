package net.bytle.tower.eraldy.module.user.model;

/**
 * A general user
 * If you are modeling an owner, you want to use the {@link net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid}
 */
public class UserGuid {


  private long localId;
  private long realmId;

  public UserGuid() {
  }
  public void setRealmId(long realmId) {
    this.realmId = realmId;
  }
  public void setLocalId(long localId) {
    this.localId = localId;
  }

  public Long getLocalId() {
    return this.localId;
  }

  public Long getRealmId() {
    return this.realmId;
  }

}
