package net.bytle.tower.eraldy.module.list.model;

import net.bytle.exception.CastException;

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

  /**
   * This name borrows its name from the exit code of a process
   */
  private final int actionCode;

  ListImportListUserAction(int code) {
    this.actionCode = code;
  }

  public static ListImportListUserAction fromCode(Integer code) throws CastException {
    for(ListImportListUserAction value: ListImportListUserAction.values()){
      if(value.getActionCode()==code){
        return value;
      }
    }
    throw new CastException("The code ("+code+") is unknown");
  }

  public int getActionCode() {
    return actionCode;
  }

}
