package net.bytle.tower.eraldy.module.realm.model;

import java.util.Objects;

public class RealmGuid {


  private long localId;

  public RealmGuid() {
  }

  /**
   * @return localId The realm id in the database
   */
  public long getLocalId() {

    return this.localId;
  }

  /**
   * @param localId The realm id in the database
   */
  public void setLocalId(long localId) {
    this.localId = localId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RealmGuid realmGuid = (RealmGuid) o;
    return localId == realmGuid.localId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(localId);
  }

}
