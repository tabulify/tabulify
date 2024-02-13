package net.bytle.email;


import com.sanctionco.jmail.InvalidEmailException;
import com.sanctionco.jmail.JMail;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import net.bytle.dns.DnsIllegalArgumentException;
import net.bytle.dns.DnsName;
import net.bytle.exception.InternalException;
import net.bytle.type.Strings;

import java.io.UnsupportedEncodingException;
import java.util.Objects;

/**
 * An email address with the domain name as {@link DnsName}
 * <p>
 * A class to handle mailbox address format ie the email RFC 822
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
  private final DnsName domain;
  private final String localPart;
  private final InternetAddress internetAddress;

  public static BMailInternetAddress of(String email, String name) throws AddressException {
    if (email == null) {
      throw new AddressException("The email given was null");
    }
    InternetAddress internetAddress = new InternetAddress(email);
    if (name != null) {
      try {
        internetAddress.setPersonal(name);
      } catch (UnsupportedEncodingException e) {
        throw new InternalException(e);
      }
    }
    return new BMailInternetAddress(internetAddress);
  }

  @Override
  public String toString() {
    return this.getInternetAddress().toString();
  }

  /**
   * @param internetAddress - a rfc822 internet address
   */
  private BMailInternetAddress(InternetAddress internetAddress) throws AddressException {

    this.internetAddress = internetAddress;

    this.emailAddress = internetAddress.getAddress();

    if (internetAddress.isGroup()) {
      throw new RuntimeException("(" + internetAddress + ") is a group email and it's not yet supported");
    }

    final String[] split = this.emailAddress.split(AT_SIGN);
    if (split.length != 2) {
      throw new AddressException("The email address does not have 2 parts (ie localname@domain)");
    }
    String absoluteName = split[1];
    try {
      this.domain = DnsName.create(absoluteName);
    } catch (DnsIllegalArgumentException e) {
      throw new AddressException("The domain (" + absoluteName + ")is not valid (");
    }
    this.localPart = split[0];

    /**
     * Set by default the personal to the local part of the email
     */
    if (this.internetAddress.getPersonal() == null) {
      try {
        this.internetAddress.setPersonal(this.localPart);
      } catch (UnsupportedEncodingException e) {
        throw new InternalException("We were unable to set the personal to the email local part", e);
      }
    }

    this.emailValidation();
  }

  public DnsName getDomainName() {
    return domain;
  }

  /**
   * @return the part before the at/arobase character
   */
  public String getLocalPart() {
    return localPart;
  }

  public static BMailInternetAddress of(String email) throws AddressException {


    return of(email, null);
  }

  public static BMailInternetAddress of(InternetAddress internetAddress) throws AddressException {
    return new BMailInternetAddress(internetAddress);
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
   * @throws AddressException - if the email is not a good one
   *                          <a href="https://stackoverflow.com/questions/624581/what-is-the-best-java-email-address-validation-method">...</a>
   *                          <p>See also
   *                          <a href=https://commons.apache.org/proper/">...</a>commons-validator/
   */
  private void emailValidation() throws AddressException {

    if (this.domain.toStringWithoutRoot().equals("localhost")) {
      throw new AddressException("The domain should not be localhost");
    }

    if (this.domain.toStringWithoutRoot().startsWith("[")) {
      throw new AddressException("The domain should not start with a [");
    }

    // user@[10.9.8.7] and user@localhost are also valid
    this.internetAddress.validate();

    /**
     * Not sure if this is needed
     */
    try {
      JMail.enforceValid(this.emailAddress);
    } catch (InvalidEmailException e) {
      // Handle invalid email
      throw new AddressException("(" + this.emailAddress + ") is not a valid email: " + e.getMessage());
    }

  }


  public InternetAddress getInternetAddress() {
    return this.internetAddress;
  }

  public String getAddress() {
    return this.emailAddress;
  }

  /**
   *
   * @return the email in lowercase format
   * ChatGpt: In practice, most mail servers and services treat both the local and domain parts of email addresses as case-insensitive.
   * Nonetheless, it's always recommended to follow the standard conventions and use lowercase characters
   * for the entire email address to avoid potential issues or confusion.
   */
  public String toNormalizedString() {
    return this.emailAddress.toLowerCase();
  }
}
