package net.bytle.tower.eraldy.module.organization.model;

import net.bytle.vertx.guid.Guid;

import java.util.Objects;

/**
 * An orga guid
 */
public class OrgaGuid extends Guid {


  /**
   * The realm id
   */
  private Long realmId;
  /**
   * The database id
   */
  private Long orgaId;

  public OrgaGuid() {

  }


  public Long getOrgaId() {
    return this.orgaId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    OrgaGuid orgaGuid = (OrgaGuid) o;
    return Objects.equals(orgaId, orgaGuid.orgaId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(orgaId);
  }

  @Override
  public String toStringLocalIds() {
    return "realm id: " + this.realmId + ", orga id: " + this.orgaId;
  }


  public Long getRealmId() {
    return realmId;
  }

  public void setRealmId(Long realmId) {
    this.realmId = realmId;
  }

  public void setOrgaId(Long localId) {
    this.orgaId = localId;
  }
}
