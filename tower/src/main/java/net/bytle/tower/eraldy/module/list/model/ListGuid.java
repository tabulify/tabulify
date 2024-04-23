package net.bytle.tower.eraldy.module.list.model;

import net.bytle.exception.InternalException;

import java.util.Objects;

public class ListGuid {


  private Long realmId;
  private long localId;

  public ListGuid() {
  }

  public Long getRealmId(){
    return this.realmId;
  }


  public long getListLocalId(Long realmId) {
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
