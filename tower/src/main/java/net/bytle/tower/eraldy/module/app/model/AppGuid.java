package net.bytle.tower.eraldy.module.app.model;

public class AppGuid {


  private Long realmId;
  private long localId;

  public AppGuid() {
  }

  public Long getRealmId(){
    return this.realmId;
  }


  public long getAppLocalId() {

    return this.localId;
  }

  public void setRealmId(long realmId) {
    this.realmId = realmId;
  }

  public void setLocalId(long localId) {
    this.localId = localId;
  }
}
