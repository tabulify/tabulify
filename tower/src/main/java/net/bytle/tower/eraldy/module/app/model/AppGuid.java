package net.bytle.tower.eraldy.module.app.model;

import java.util.Objects;

public class AppGuid {


  private Long realmId;
  private long localId;

  public AppGuid() {
  }

  public Long getRealmId(){
    return this.realmId;
  }

  /**
   * @return The app identifier in the realm (without the realm, the id may have duplicate)
   */
  public long getAppLocalId() {

    return this.localId;
  }

  public void setRealmId(long realmId) {
    this.realmId = realmId;
  }

  /**
   * @param localId The app identifier in the realm (without the realm, the id may have duplicate)
   */
  public void setLocalId(long localId) {
    this.localId = localId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AppGuid appGuid = (AppGuid) o;
    return localId == appGuid.localId && Objects.equals(realmId, appGuid.realmId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(realmId, localId);
  }

}
