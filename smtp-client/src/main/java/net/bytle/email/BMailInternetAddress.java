package net.bytle.email;

import com.sanctionco.jmail.InvalidEmailException;
import com.sanctionco.jmail.JMail;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import net.bytle.exception.CastException;
import net.bytle.type.Casts;
import net.bytle.type.Strings;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A class to handle mail box format
 * ie the email RFC 822
 * where an email address can be specified in a mailbox format
 * `<yolo yol@email.com>`
 * It wraps a {@link jakarta.mail.internet.MimeMessage}
 * <p>
 * Email fields are Strings using the common formats for email with or without real name.
 * Example of valid values
 * `username@example.com`
 * `username@example.com (Firstname Lastname)`
 * `Firstname Lastname <username@example.com>`
 */
public class BMailInternetAddress {

  private static final String AT_SIGN = "@";
  private final String emailAddress;
  private final String domain;
  private final String localPart;
  private final InternetAddress internetAddress;

  @Override
  public String toString() {
    return this.getInternetAddress().toString();
  }

  /**
   * @param internetAddress - a rfc822 internet address
   */
  private BMailInternetAddress(InternetAddress internetAddress) {

    this.internetAddress = internetAddress;

    this.emailAddress = internetAddress.getAddress();

    if (internetAddress.isGroup()) {
      throw new RuntimeException("(" + internetAddress + ") is a group email and it's not yet supported");
    }

    final String[] split = this.emailAddress.split(AT_SIGN);
    if (split.length == 2) {
      this.domain = split[1];
      this.localPart = split[0];
    } else {
      this.domain = "";
      this.localPart = "";
    }


  }

  /**
   * @throws AddressException - if the address is not valid
   */
  public BMailInternetAddress validate() throws AddressException {
    // user@[10.9.8.7] and user@localhost are also valid
    this.internetAddress.validate();
    if (this.emailAddress.split(AT_SIGN).length != 2) {
      throw new AddressException("(" + this.emailAddress + ") is not a valid email");
    }
    try {
      JMail.enforceValid(this.emailAddress);
    } catch (InvalidEmailException e) {
      // Handle invalid email
      throw new AddressException("(" + this.emailAddress + ") is not a valid email: " + e.getMessage());
    }
    return this;
  }

  public String getDomain() {
    return domain;
  }

  /**
   * @return the part before the at/arobase character
   */
  public String getLocalPart() {
    return localPart;
  }

  public static BMailInternetAddress of(String email) throws AddressException {
    InternetAddress internetAddress = new InternetAddress(email);

    return new BMailInternetAddress(internetAddress);
  }

  public static BMailInternetAddress of(InternetAddress internetAddress) {
    return new BMailInternetAddress(internetAddress);
  }

  public List<String> getDomainParts() {

    return Arrays.asList(domain.split("\\."));

  }

  public String getRootDomain() {

    List<String> rootDomainParts = IntStream
      .range(0, 2)
      .mapToObj(i -> getDomainParts().get(getDomainParts().size() - 1 - i))
      .sorted(Collections.reverseOrder())
      .collect(Collectors.toList());

    return String.join(".", rootDomainParts);

  }

  public Integer getDomainDots() {
    return getDomainParts().size() - 1;
  }

  public Integer getLocalPartDigitCount() {
    return Strings.createFromString(localPart).getDigitCount();
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BMailInternetAddress BMailInternetAddress1 = (BMailInternetAddress) o;
    return Objects.equals(emailAddress, BMailInternetAddress1.emailAddress);
  }

  @Override
  public int hashCode() {
    return Objects.hash(emailAddress);
  }

  /**
   * @throws AddressException - if the email is not a good external one
   *                          <a href="https://stackoverflow.com/questions/624581/what-is-the-best-java-email-address-validation-method">...</a>
   *                          <p>
   *                          <a href="         See also https://commons.apache.org/proper/">...</a>commons-validator/
   */
  public void externalEmailValidation() throws AddressException {
    if (this.domain.equals("localhost")) {
      throw new AddressException("The domain should not be localhost");
    }
    if (this.domain.startsWith("[")) {
      throw new AddressException("The domain should not start with a [");
    }
    List<String> domainParts = this.getDomainParts();
    if (domainParts.size() < 2) {
      throw new AddressException("The domain should have at minimal a TLD domain (.com, ...)");
    }


    for (String part : domainParts) {

      try {
        Casts.cast(part, Integer.class);
        throw new AddressException("A domain part should not be a number. The part (" + part + ") is a number.");
      } catch (CastException e) {
        // should throw
      }
      if (part.length() <= 1) {
        throw new AddressException("A domain part should have at minimum 2 characters. The part (" + part + ") has less than 2 characters.");
      }
      try {
        String firstLetter = part.substring(0, 1);
        Casts.cast(firstLetter, Integer.class);
        throw new AddressException("A domain part should not start with a number. The part (" + part + ") start with the number " + firstLetter + ".");
      } catch (CastException e) {
        // should throw
      }

    }


  }


  public InternetAddress getInternetAddress() {
    return this.internetAddress;
  }

  public String getAddress() {
    return this.emailAddress;
  }

}
