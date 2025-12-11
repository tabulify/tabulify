package com.tabulify.type;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EmailAddressTest {

  @Test
  void domainNoTldValidation() {
    // no tld
    String noTld = "com@com";
    Assertions.assertThrows(
      EmailCastException.class,
      () -> EmailAddress.of(noTld),
      "The domain should have at minimal a TLD domain (.com, ...)"
    );
  }

  @Test
  void doesNotStartWithValidation() {
    String finalEmailString2 =  "user@[10.9.8.7]";
    Assertions.assertThrows(
      EmailCastException.class,
      ()->EmailAddress.of(finalEmailString2),
      "The domain should not start with a ["
    );
  }

  @Test()
  public void externalEmailValidation() {

    // no tld
    String emailString = "com@com.com";

    try {
      EmailAddress.of(emailString);
    } catch (EmailCastException e) {
      throw new RuntimeException("Should not throw");
    }

    // Address should not start with a dot
    // Valid for gmail but yeah, not human
    String dotEmailString  = ".foo@bar.tld";
    Assertions.assertThrows(
      EmailCastException.class,
      ()->EmailAddress.of(dotEmailString),
      "Local address starts with dot"
    );


    // no final domain
    // same with `abc`
    emailString = "com.";
    String finalEmailString = emailString;
    Assertions.assertThrows(
      EmailCastException.class,
      () -> EmailAddress.of(finalEmailString),
      "Missing final '@domain'"
    );

    emailString = "user@10.9.8.7";
    String finalEmailString1 = emailString;
    Assertions.assertThrows(
      EmailCastException.class,
      () -> EmailAddress.of(finalEmailString1),
      "A domain part should not be a number. The part (10) is a number."
    );


  }
}
