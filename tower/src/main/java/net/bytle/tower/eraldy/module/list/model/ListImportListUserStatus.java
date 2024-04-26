package net.bytle.tower.eraldy.module.list.model;

import net.bytle.tower.eraldy.model.manual.Status;
import net.bytle.type.Strings;

public enum ListImportListUserStatus implements Status {

  /**
   * Nothing has been done to the user
   * on this list during the import
   */
  NOTHING(0,"The user has no been modified"),

  ADDED(1,"The user has been added to the list"),

  DELETED(2,"The user has been deleted from the list")
  ;

  private final int code;
  private final String desc;

  ListImportListUserStatus(int code, String s) {
    this.code = code;
    this.desc = s;
  }

  public int getCode() {
    return code;
  }

  @Override
  public int getOrder() {
    return this.code;
  }

  @Override
  public String getName() {
    return Strings.createFromString(this.name()).toFirstLetterCapitalCase().toString();
  }

  @Override
  public String getDescription() {
    return this.desc;
  }

}
