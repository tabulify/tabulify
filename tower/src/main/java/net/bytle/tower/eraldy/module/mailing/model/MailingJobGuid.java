package net.bytle.tower.eraldy.module.mailing.model;

public class MailingJobGuid {


  /**
   * The database realm id
   */
  private Long realmId;
  /**
   * The database local id
   */
  private long localId;

  public MailingJobGuid() {
  }

  public Long getRealmId(){
    return this.realmId;
  }

  /**
   * @return localId The mailing identifier in the realm scope. Without the realm, this id has duplicates.
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


}
