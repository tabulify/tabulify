package net.bytle.tower.eraldy.module.organization.model;

/**
 *
 */
public enum OrgaRole {


  OWNER(1);


  private final int roleId;

  OrgaRole(int roleId) {
    this.roleId = roleId;
  }

  public int getId() {
    return this.roleId;
  }

}
