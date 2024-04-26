package net.bytle.tower.eraldy.api.implementer.flow;

import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.model.manual.Status;
import net.bytle.type.Strings;

/**
 * The action done on the user
 * if it exists
 */
public enum ListImportUserAction implements Status {

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

  @Override
  public int getCode() {
    return this.actionCode;
  }

  @Override
  public int getOrder() {
    return this.actionCode;
  }

  @Override
  public String getName() {
    return Strings.createFromString(this.name()).toFirstLetterCapitalCase().toString();
  }

  @Override
  public String getDescription() {
    return this.getName();
  }
}
