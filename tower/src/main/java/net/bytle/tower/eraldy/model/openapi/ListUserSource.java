package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonValue;
import net.bytle.tower.eraldy.model.manual.Status;
import net.bytle.type.Strings;

/**
* How the user has been added to the list
*/
public enum ListUserSource implements Status {

  EMAIL(0,"The user has clicked on a email link"),

  OAUTH(1,"The user has clicked on a oAuth button"),

  IMPORT(2,"The user has been imported");

  private final int value;
  private final String desc;

  ListUserSource(int value, String s) {

    this.value = value;
    this.desc = s;
  }

  @JsonValue
  public Integer getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public static ListUserSource fromValue(Integer value) {
    for (ListUserSource b : ListUserSource.values()) {
      if (b.value == value) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }

  @Override
  public int getCode() {
    return this.value;
  }

  @Override
  public int getOrder() {
    return this.value;
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
