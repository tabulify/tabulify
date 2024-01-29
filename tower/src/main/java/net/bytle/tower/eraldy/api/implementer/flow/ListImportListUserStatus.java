package net.bytle.tower.eraldy.api.implementer.flow;

public enum ListImportListUserStatus {

  /**
   * Nothing has been done to the user
   * on this list during the import
   */
  NOTHING(0),
  /**
   * The user has been added to the list
   */
  ADDED(1),
  /**
   * The user has been deleted from the list
   */
  DELETED(2)
  ;

  private final int code;

  ListImportListUserStatus(int code) {
    this.code = code;
  }

  public int getCode() {
    return code;
  }

}
