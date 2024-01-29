package net.bytle.tower.eraldy.api.implementer.flow;

public enum ListImportUserStatus {

  /**
   * Nothing has been done to the user
   * during the import
   */
  NOTHING(0),
  /**
   * The user has been created
   */
  CREATED(1),
  /**
   * The user properties has been updated
   */
  UPDATED(2);

  private final int code;

  ListImportUserStatus(int code) {
    this.code = code;
  }

  public int getCode() {
    return code;
  }

}
