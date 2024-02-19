package net.bytle.tower.eraldy.api.implementer.flow;

import net.bytle.exception.CastException;

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

  public static ListImportUserAction fromCode(Integer code) throws CastException {
    for(ListImportUserAction value: ListImportUserAction.values()){
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
