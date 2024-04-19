package net.bytle.vertx;

import net.bytle.type.EmailAddress;
import net.bytle.type.EmailCastException;

public class SysAdmin {
  public static EmailAddress getEmail() {
      try {
          return EmailAddress.of("nico@bytle.net");
      } catch (EmailCastException e) {
          throw new RuntimeException("The mail is a literal and should be good",e);
      }
  }

}
