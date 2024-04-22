package net.bytle.tower.eraldy.module.app.model;

import net.bytle.exception.InternalException;

import java.util.Objects;

public class AppGuid {


  private Long realmId;
  private long localId;

  public AppGuid() {
  }

  public Long getRealmId(){
    return this.realmId;
  }


  public long getAppLocalId(Long realmId) {
    if(!Objects.equals(this.realmId, realmId)){
      throw new InternalException("The realm ids does not match");
    }
    return this.localId;
  }

  public void setRealmId(long realmId) {
    this.realmId = realmId;
  }

  public void setLocalId(long localId) {
    this.localId = localId;
  }
}
