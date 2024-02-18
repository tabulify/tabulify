package net.bytle.tower.eraldy.api.implementer.flow;

/**
 * The action done on the user in the list
 */
public enum ListImportListUserAction {

  /**
   * Add
   */
  IN(0),
  /**
   * Delete
   */
  OUT(1);

  private final int actionCode;

  ListImportListUserAction(int code) {
    this.actionCode = code;
  }

  public int getActionCode() {
    return actionCode;
  }

}
