package net.bytle.tower.eraldy.module.list.model;

import net.bytle.vertx.guid.Guid;

public class ListGuid extends Guid {


  private Long realmId;
  private long localId;

  public ListGuid() {
  }

  public Long getRealmId(){
    return this.realmId;
  }

  /**
   * @return localId The list identifier in the realm scope. Without the realm, this id has duplicates.
   */
  public long getLocalId() {

    return this.localId;
  }

  public void setRealmId(long realmId) {
    this.realmId = realmId;
  }

  /**
   * @param localId The list identifier in the realm scope. Without the realm, this id has duplicates.
   */
  public void setLocalId(long localId) {
    this.localId = localId;
  }

  public String toStringLocalIds() {
    return "listId=" + localId +", realmId=" + realmId;
  }

}
