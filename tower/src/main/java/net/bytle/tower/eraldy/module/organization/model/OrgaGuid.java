package net.bytle.tower.eraldy.module.organization.model;

import java.util.Objects;

/**
 * An orga guid
 */
public class OrgaGuid  {


  private Long localId;

  public OrgaGuid() {
    super();
  }


  public void setLocalId(Long localId) {

    this.localId = localId;

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
}
