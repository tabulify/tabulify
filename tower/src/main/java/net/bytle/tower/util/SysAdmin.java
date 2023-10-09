package net.bytle.tower.util;

import net.bytle.tower.eraldy.model.openapi.User;

public class SysAdmin {
  /**
   * The system admin that receive the errors
   * and melding
   */
  public static final User ADMIN_USER;

  /**
   * The system user (that sends the email or message)
   */
  public static final User SYS_USER;

  static {

    ADMIN_USER = new User();
    ADMIN_USER.setName("Nico");
    ADMIN_USER.setEmail("nico@bytle.net");

    SYS_USER = new User();
    SYS_USER.setName("beau");
    SYS_USER.setEmail("beau@bytle.net");

  }

}
