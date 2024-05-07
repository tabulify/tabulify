package net.bytle.tower.eraldy.module.realm.model;

import net.bytle.vertx.guid.Guid;

import java.util.Objects;

/**
 * A general user
 * If you are modeling an owner, you want to use the {@link net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid}
 */
public class UserGuid extends Guid {


  private long userId;
  protected long realmId;

  public UserGuid() {
  }
  public void setRealmId(Long realmId) {
    assert realmId != null : "Local realm Id cannot be null";
    this.realmId = realmId;

  }

  /**
   * @param userId The user id in the realm in the database (ie local to the realm)  We have called it local to avoid to really indicate that is not the true id.  You can't check with this id if this is the same user as this is the id inside the realm, there is other id with the same value in another realm.
   */
  public void setUserId(Long userId) {
    assert userId != null : "User Id cannot be null";
    this.userId = userId;
  }

  /**
   * @return localId The user id in the realm in the database (ie local to the realm)  We have called it local to avoid to really indicate that is not the true id.  You can't check with this id if this is the same user as this is the id inside the realm, there is other id with the same value in another realm.
   */
  public long getUserId() {
    return this.userId;
  }

  public long getRealmId() {
    return this.realmId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof UserGuid)) return false;
    UserGuid userGuid = (UserGuid) o;
    return userId == userGuid.userId && realmId == userGuid.realmId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, realmId);
  }

  @Override
  public String toStringLocalIds() {
    return "localId=" + userId +", realmId=" + realmId;
  }

}
