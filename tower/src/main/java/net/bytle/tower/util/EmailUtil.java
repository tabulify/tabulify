package net.bytle.tower.util;

import io.vertx.json.schema.ValidationException;
import net.bytle.email.BMailAddress;

import javax.mail.internet.AddressException;

public class EmailUtil {

  public static void validateEmail(String email, String attribute) {
    /**
     * Email validation
     */
    if (email == null) {
      throw ValidationException.create("No email was provided", attribute, null);
    }

    try {
      BMailAddress.of(email)
        .externalEmailValidation();
    } catch (AddressException e) {
      throw ValidationException.create("The email is not valid. Error: " + e.getMessage(), attribute, email);
    }
  }

}
