package net.bytle.tower.eraldy.module.mailing.model;

import net.bytle.vertx.guid.Guid;

public class MailingItemGuid extends Guid {


  /**
   * The database realm id
   */
  private Long realmId;
  /**
   * The database mailing local id
   */
  private long mailingId;

  /**
   * The database user local id
   */
  private long userId;


  public MailingItemGuid() {
  }

  public Long getRealmId(){
    return this.realmId;
  }

  /**
   * @return localId The mailing identifier in the realm scope. Without the realm, this id has duplicates.
   */
  public long getMailingId() {

    return this.mailingId;
  }

  public void setRealmId(long realmId) {
    this.realmId = realmId;
  }

  /**
   * @param mailingId The list identifier in the realm scope. Without the realm, this id has duplicates.
   */
  public void setMailingId(long mailingId) {
    this.mailingId = mailingId;
  }


  public long getUserId() {
    return userId;
  }

  public void setUserId(long userId) {
    this.userId = userId;
  }

  @Override
  public String toStringLocalIds() {
    return "realmId=" + realmId +
      ", userId=" + userId +
      ", mailingId=" + mailingId;
  }
}
