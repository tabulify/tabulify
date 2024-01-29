package net.bytle.tower.eraldy.api.implementer.flow;

/**
 * The action done on the user
 * if it exists
 */
public enum ListImportUserAction {

  /**
   * The properties of the user are not updated
   */
  NOTHING(0),
  /**
   * The properties of the user are updated
   * (if they are different)
   */
  UPDATE(1);

  private final int actionCode;

  ListImportUserAction(int code) {
    this.actionCode = code;
  }

  public int getActionCode() {
    return actionCode;
  }

}
