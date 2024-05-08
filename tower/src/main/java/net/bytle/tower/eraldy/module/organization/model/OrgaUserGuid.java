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


  /**
   * The database id
   */
  private long organizationId;

  public OrgaUserGuid() {
    super();
  }



  public void setOrganizationId(Long organizationId) {

    this.organizationId = organizationId;

  }


  public long getOrganizationId() {

    return this.organizationId;

  }


  public OrgaGuid getOrgaGuid() {
    OrgaGuid orgaGuid = new OrgaGuid();
    orgaGuid.setRealmId(this.getRealmId());
    orgaGuid.setOrgaId(this.getOrganizationId());
    return orgaGuid;
  }

}
