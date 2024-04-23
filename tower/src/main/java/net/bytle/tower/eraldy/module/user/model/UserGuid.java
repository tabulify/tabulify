package net.bytle.tower.eraldy.module.user.model;

import java.util.Objects;

/**
 * A general user
 * If you are modeling an owner, you want to use the {@link net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid}
 */
public class UserGuid {


  private long localId;
  private long realmId;

  public UserGuid() {
  }
  public void setRealmId(Long realmId) {
    assert realmId != null : "Local realm Id cannot be null";
    this.realmId = realmId;

  }
  public void setLocalId(Long localId) {
    assert localId != null : "Local user Id cannot be null";
    this.localId = localId;
  }

  public Long getLocalId() {
    return this.localId;
  }

  public Long getRealmId() {
    return this.realmId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserGuid userGuid = (UserGuid) o;
    return localId == userGuid.localId && realmId == userGuid.realmId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(localId, realmId);
  }

}
