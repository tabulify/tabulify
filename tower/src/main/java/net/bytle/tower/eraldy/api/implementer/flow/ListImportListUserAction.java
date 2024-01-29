package net.bytle.tower.eraldy.api.implementer.flow;

/**
 * The action done on the user in the list
 */
public enum ListImportListUserAction {
  IN(0),
  OUT(1);

  private final int actionCode;

  ListImportListUserAction(int code) {
    this.actionCode = code;
  }

  public int getActionCode() {
    return actionCode;
  }

}
