package net.bytle.tower.eraldy.module.realm.model;

import java.util.Objects;

public class RealmGuid {


  private final long localId;

  /**
   * @param realmId The realm id in the database
   */
  public RealmGuid(long realmId) {
    this.localId = realmId;
  }

  /**
   * @return localId The realm id in the database
   */
  public long getLocalId() {

    return this.localId;
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

  @Override
  public String toString() {
    return String.valueOf(localId);
  }

}
