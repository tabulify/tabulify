package net.bytle.tower.eraldy.module.organization.model;

import net.bytle.vertx.guid.Guid;

import java.util.Objects;

/**
 * An orga guid
 */
public class OrgaGuid extends Guid {


  /**
   * The database id
   */
  private final Long localId;

  /**
   *
   * The public hash
   */
  private String publicHash;

  public OrgaGuid(Long orgaId) {
    this.localId = orgaId;
  }


  public Long getLocalId() {
    return this.localId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    OrgaGuid orgaGuid = (OrgaGuid) o;
    return Objects.equals(localId, orgaGuid.localId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(localId);
  }

  @Override
  public String toStringLocalIds() {
    return String.valueOf(this.localId);
  }


  public String getPublicHash() {
    return this.publicHash;
  }

  public void setPublicHash(String publicHash) {
    this.publicHash = publicHash;
  }

}
