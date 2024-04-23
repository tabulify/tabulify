package net.bytle.tower.eraldy.module.list.model;

public class ListGuid {


  private Long realmId;
  private long localId;

  public ListGuid() {
  }

  public Long getRealmId(){
    return this.realmId;
  }


  public long getListLocalId() {

    return this.localId;
  }

  public void setRealmId(long realmId) {
    this.realmId = realmId;
  }

  public void setLocalId(long localId) {
    this.localId = localId;
  }
}
