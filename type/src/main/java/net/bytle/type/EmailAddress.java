package net.bytle.type;

import net.bytle.exception.CastException;

import java.util.Objects;

/**
 * An email address (not an Internet Address)
 * <p>
 * Usage: handle Email validation in DNS records.
 * <p>
 * If you use SMTP (Internet Address), you should use the BMailInternetAddress that represents
 * an Internet Address (ie Jakarta package representations of an email address and name)
 * <p>
 * Why, do we have extracted the email address.
 * It's difficult to not create a circular dependencies between DNS and the Smtp Package. ie
 * * There is an email in dmarc, we need it then in DNS to verify that this an email.
 * * And if the smtp package does need it also.
 */
public class EmailAddress {


  private final String mailAddress;
  private final DnsName domain;
  private final String localPart;

  public EmailAddress(String mailAddress) throws EmailCastException {
    this.mailAddress = mailAddress;
    final String[] split = mailAddress.split("@");
    if (split.length != 2) {
      throw new EmailCastException("The email address does not have 2 parts (ie localname@domain)");
    }
    String absoluteName = split[1];
    try {
      this.domain = DnsName.create(absoluteName);
    } catch (CastException e) {
      throw new EmailCastException("The domain (" + absoluteName + ")is not valid (");
    }
    this.localPart = split[0];


    /**
     * Basic validation
     * user@[10.9.8.7] and user@localhost are also valid
     * but we don't accept them
     */
    if (this.domain.toStringWithoutRoot().equals("localhost")) {
      throw new EmailCastException("The domain should not be localhost");
    }

    if (this.domain.toStringWithoutRoot().startsWith("[")) {
      throw new EmailCastException("The domain should not start with a [");
    }
  }

  public static EmailAddress of(String mail) throws EmailCastException {
    return new EmailAddress(mail);
  }

  @Override
  public String toString() {
    return toNormalizedString();
  }

  public DnsName getDomainName() {
    return this.domain;
  }

  @SuppressWarnings("unused")
  public String getLocalPart() {
    return localPart;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EmailAddress that = (EmailAddress) o;
    return Objects.equals(mailAddress, that.mailAddress);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mailAddress);
  }

  public Integer getLocalPartDigitCount() {
    return Strings.createFromString(localPart).getDigitCount();
  }

  /**
   *
   * @return the email in lowercase format
   * ChatGpt: In practice, most mail servers and services treat both the local and domain parts of email addresses as case-insensitive.
   * Nonetheless, it's always recommended to follow the standard conventions and use lowercase characters
   * for the entire email address to avoid potential issues or confusion.
   */
  public String toNormalizedString() {
    return this.mailAddress.toLowerCase();
  }

}
