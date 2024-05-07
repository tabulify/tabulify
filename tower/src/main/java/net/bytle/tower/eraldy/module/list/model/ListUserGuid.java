package net.bytle.tower.eraldy.module.list.model;

import net.bytle.tower.eraldy.module.realm.model.UserGuid;
import net.bytle.vertx.guid.Guid;

public class ListUserGuid extends Guid {


  public static final String GUID_PREFIX = "liu";
  private long realmId;
  private long userId;
  private long listId;

  public ListUserGuid() {
    super();
  }

  public long getRealmId(){
    return this.realmId;
  }

  /**
   * @return localId The local user id
   */
  public long getUserId() {

    return this.userId;
  }

  public void setRealmId(long realmId) {
    this.realmId = realmId;
  }

  /**
   * @param userId The list identifier in the realm scope. Without the realm, this id has duplicates.
   */
  public void setUserId(long userId) {
    this.userId = userId;
  }

  public long getListId() {
    return listId;
  }

  public void setListId(long listId) {
    this.listId = listId;
  }

  @Override
  public String toStringLocalIds() {
    return "realmId=" + realmId +
      ", userId=" + userId +
      ", listId=" + listId;
  }

  public UserGuid toUserGuid() {
    UserGuid userGuid = new UserGuid();
    userGuid.setRealmId(this.realmId);
    userGuid.setUserId(this.userId);
    return userGuid;
  }
}
