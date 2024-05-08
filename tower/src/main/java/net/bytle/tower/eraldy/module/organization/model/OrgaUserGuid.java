package net.bytle.tower.eraldy.module.organization.model;

import net.bytle.tower.eraldy.module.realm.model.UserGuid;

/**
 * An orga user guid is:
 * * a user guid
 * * in an organization
 * <p>
 * They are 2 differents guid but for an auth perspective, they are the same.
 * By not extending, you can't check this equality (they are the same user)
 */
public class OrgaUserGuid extends UserGuid {


  private final long orgaId;

  private OrgaUserGuid(Builder builder) {
    super(new UserGuid.Builder()
      .setUserId(builder.userId)
      .setRealmId(builder.realmId)
    );
    assert builder.organizationId != null : "Orga Id cannot be null when building a OrgaUserGuid";

    orgaId = builder.organizationId;
  }




  public long getOrganizationId() {

    return this.orgaId;

  }


  public OrgaGuid getOrgaGuid() {
    OrgaGuid orgaGuid = new OrgaGuid();
    orgaGuid.setRealmId(this.getRealmId());
    orgaGuid.setOrgaId(this.getOrganizationId());
    return orgaGuid;
  }

  public static class Builder {

    private Long userId;
    protected Long realmId;
    /**
     * The database id
     */
    private Long organizationId;
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

    /**
     * @param orgaId The user id in the realm in the database (ie local to the realm)  We have called it local to avoid to really indicate that is not the true id.  You can't check with this id if this is the same user as this is the id inside the realm, there is other id with the same value in another realm.
     */
    public Builder setOrgaId(Long orgaId) {

      this.organizationId = orgaId;
      return this;
    }

    public OrgaUserGuid build(){
      return new OrgaUserGuid(this);
    }

  }

}
