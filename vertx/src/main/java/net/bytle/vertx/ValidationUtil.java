package net.bytle.vertx;


import io.vertx.json.schema.ValidationException;
import jakarta.mail.internet.AddressException;
import net.bytle.email.BMailInternetAddress;

public class ValidationUtil {

  public static void validateEmail(String email, String attribute) {
    /**
     * Email validation
     */
    if (email == null) {
      throw ValidationException.create("No email was provided", attribute, null);
    }

    try {
      BMailInternetAddress.of(email)
        .externalEmailValidation();
    } catch (AddressException e) {
      throw ValidationException.create("The email is not valid. Error: " + e.getMessage(), attribute, email);
    }
  }

}
