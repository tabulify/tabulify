package net.bytle.tower.eraldy.module.auth.model;


import net.bytle.vertx.guid.Guid;

import java.util.Objects;

public class CliGuid extends Guid {


  private Long realmId;
  private long localId;

  public CliGuid() {
  }

  public Long getRealmId(){
    return this.realmId;
  }

  /**
   * @return localId The cli identifier in the realm scope. Without the realm, this id has duplicates.
   */
  public long getLocalId() {

    return this.localId;
  }

  public void setRealmId(long realmId) {
    this.realmId = realmId;
  }

  /**
   * @param localId The cli identifier in the realm scope. Without the realm, this id has duplicates.
   */
  public void setLocalId(long localId) {
    this.localId = localId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CliGuid cliGuid = (CliGuid) o;
    return localId == cliGuid.localId && Objects.equals(realmId, cliGuid.realmId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(realmId, localId);
  }


  public String toStringLocalIds() {
    return "`cli`Id=" + localId +", realmId=" + realmId;
  }
}
