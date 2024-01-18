package net.bytle.vertx.auth;

import jakarta.mail.internet.AddressException;
import net.bytle.email.BMailInternetAddress;
import net.bytle.exception.NotFoundException;
import net.bytle.type.Strings;

/**
 * Auth and non-auth users utils
 */
public class AuthUserUtils {
  /**
   * Example: in Nicolas GERARD, the given name is Nicolas because it's not fully uppercase
   * @param firstName         - the default first name
   * @param possibleFirstName - the other first name candidate
   * @return the first name based on the case. If otherName is lowercase and defaultName is uppercase, otherName is returned)
   */
  public static String getGivenNameFromCase(String firstName, String possibleFirstName) {
    if (possibleFirstName == null) {
      return firstName;
    }
    if (Strings.isUpperCase(firstName) && !Strings.isUpperCase(possibleFirstName)) {
      return possibleFirstName;
    }
    return firstName;
  }

  /**
   * Example: in Nicolas GERARD, the name is GERARD because it's uppercase
   * @param familyName         - the default family name
   * @param possibleFamilyName - the other family name candidate
   * @return the first name based on the case. If otherName is lowercase and defaultName is uppercase, otherName is returned)
   */
  public static String getFamilyNameFromCase(String familyName, String possibleFamilyName) {
    if (possibleFamilyName == null) {
      return familyName;
    }
    if (!Strings.isUpperCase(familyName) && Strings.isUpperCase(possibleFamilyName)) {
      return possibleFamilyName;
    }
    return familyName;
  }

  public static String getNameOrNameFromEmail(String name, String email) throws NotFoundException, AddressException {

    if (name != null) {
      return name;
    }

    if (email == null) {
      // should not happen as an email is mandatory
      throw new NotFoundException("No name could be found for this user");
    }
    return BMailInternetAddress.of(email).getLocalPart();

  }
}
