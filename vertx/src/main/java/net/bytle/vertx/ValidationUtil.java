package net.bytle.vertx;


import io.vertx.json.schema.ValidationException;
import jakarta.mail.internet.AddressException;
import net.bytle.email.BMailInternetAddress;
import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.exception.IllegalStructure;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.auth.AuthQueryProperty;

public class ValidationUtil {

  /**
   *
   * The validation is now part of the constructor
   */
  @Deprecated
  public static BMailInternetAddress validateEmail(String email, String attribute) {
    /**
     * Email validation
     */
    if (email == null) {
      throw ValidationException.create("No email was provided", attribute, null);
    }

    try {
      return BMailInternetAddress.of(email);
    } catch (AddressException e) {
      throw ValidationException.create("The email is not valid. Error: " + e.getMessage(), attribute, email);
    }
  }

  public static UriEnhanced validateAndGetRedirectUriAsUri(String redirectUri)  {
    if (redirectUri == null) {
      throw IllegalArgumentExceptions.createWithInputNameAndValue("The redirect Uri cannot be null", AuthQueryProperty.REDIRECT_URI.toString(), null);
    }
    UriEnhanced uri;
    try {
      uri = UriEnhanced.createFromString(redirectUri);
    } catch (IllegalStructure e) {
      throw IllegalArgumentExceptions.createWithInputNameAndValue("The redirect Uri is not a valid URI", AuthQueryProperty.REDIRECT_URI.toString(), redirectUri);
    }
    if (uri.getHost() == null) {
      throw IllegalArgumentExceptions.createWithInputNameAndValue("The redirect Uri should be absolute", AuthQueryProperty.REDIRECT_URI.toString(), redirectUri);
    }
    return uri;
  }
}
