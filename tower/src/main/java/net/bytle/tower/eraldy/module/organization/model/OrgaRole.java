package net.bytle.tower.eraldy.module.organization.model;

import net.bytle.exception.NotFoundException;


public enum OrgaRole {


  OWNER(1);


  private final int roleId;

  OrgaRole(int roleId) {
    this.roleId = roleId;
  }

  public static OrgaRole fromRoleIdFailSafe(Integer roleId) {
      try {
          return fromRoleId(roleId);
      } catch (NotFoundException e) {
          throw new RuntimeException(e);
      }
  }

  public int getId() {
    return this.roleId;
  }

  public static OrgaRole fromRoleId(int roleId) throws NotFoundException {
    for (OrgaRole value : values()) {
      if (value.roleId == roleId) {
        return value;
      }
    }
    throw new NotFoundException("The id (" + roleId + ") is not a valid role id");
  }


}
