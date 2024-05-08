package net.bytle.tower.eraldy.module.realm.model;

import net.bytle.vertx.guid.Guid;

import java.util.Objects;

/**
 * A general user
 * If you are modeling an owner, you want to use the {@link net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid}
 */
public class UserGuid extends Guid {


  private final long userId;
  private final long realmId;

  protected UserGuid(Builder builder) {
    assert builder.userId != null : "User Id cannot be null when building a UserGuid";
    userId = builder.userId;
    assert builder.realmId != null : "Realm Id cannot be null when building a UserGuid";
    realmId = builder.realmId;
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


  public static class Builder {

    private Long userId;
    protected Long realmId;
    public Builder setRealmId(Long realmId) {

      this.realmId = realmId;
      return this;

    }

    /**
     * @param userId The user id in the realm in the database (ie local to the realm)  We have called it local to avoid to really indicate that is not the true id.  You can't check with this id if this is the same user as this is the id inside the realm, there is other id with the same value in another realm.
     */
    public Builder setUserId(Long userId) {

      this.userId = userId;
      return this;
    }

    public UserGuid build(){

      return new UserGuid(this);
    }

  }
}
