package net.bytle.tower.eraldy.module.realm.model;

import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.model.manual.Status;

/**
 * The status of the mailing
 * (ie SMTP status)
 */
public enum UserStatus implements Status {



  OK(0, 1, "Ok", "User is valid");



  private final int code;
  private final String name;
  private final String description;
  private final int order;

  UserStatus(int code, int order, String name, String description) {
    this.order = order;
    this.code = code;
    this.name = name;
    this.description = description;
  }


  public static UserStatus fromStatusCode(int statusCode) throws NotFoundException {
    for (UserStatus value : values()) {
      if (value.code == statusCode) {
        return value;
      }
    }
    throw new NotFoundException("The code (" + statusCode + ") is not a valid mailing status");
  }

  public static UserStatus fromStatusCodeFailSafe(Integer status) {
      try {
          return fromStatusCode(status);
      } catch (NotFoundException e) {
          throw new RuntimeException("The user status ("+status+") is unknown",e);
      }
  }

  @Override
  public String toString() {
    return code + " (" + name + ")";
  }

  public int getCode() {
    return this.code;
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public String getDescription() {
    return this.description;
  }


}
