package net.bytle.smtp;

import net.bytle.dns.DnsBlockList;
import net.bytle.dns.DnsBlockListQuery;
import net.bytle.dns.DnsBlockListResponseCode;
import net.bytle.dns.DnsName;
import net.bytle.email.BMailInternetAddress;
import net.bytle.exception.IllegalStructure;

import java.util.Map;

/**
 * Filtering third party incoming transaction
 * We follow
 * <a href="https://www.spamhaus.org/whitepapers/effective_filtering/">...</a>
 * See also:
 * <a href="https://docs.spamhaus.com/datasets/docs/source/40-real-world-usage/smtp/000-intro.html">Filtering SMTP</a>
 * <p>
 */
public class SmtpFiltering {

  /**
   * When the session is already authenticated,
   * there is no need to filter (ie check ip, domain, ....)
   */
  private static boolean shouldNotBeFiltered(SmtpSession smtpSession) {
    if (smtpSession.isAuthenticatedOrIsLocalhost()) {
      return true;
    }
    return smtpSession.getSmtpService().isAuthRequired();
  }

  public static void checkIfDomainIsNotBlocked(SmtpSession smtpSession, BMailInternetAddress emailAddress) throws SmtpException {
    if (smtpSession.getSmtpService().getSmtpServer().isDnsBlockListDisabled()) {
      return;
    }
    if (shouldNotBeFiltered(smtpSession)) {
      return;
    }
    String domain = emailAddress.getDomain();
    Map.Entry<DnsBlockList, DnsBlockListResponseCode> isBlocked = DnsBlockListQuery.forDomain(domain)
      .query()
      .getFirstRecord();
    DnsBlockListResponseCode value = isBlocked.getValue();
    if (value.getBlocked()) {
      throw SmtpFiltering.getException("Domain " + domain + " blacklisted by " + isBlocked.getKey() + " (Reason: " + value.getDescription() + ")");
    }
  }

  public static void checkIp(SmtpSession smtpSession) throws SmtpException {
    if (smtpSession.getSmtpService().getSmtpServer().isDnsBlockListDisabled()) {
      return;
    }
    if (shouldNotBeFiltered(smtpSession)) {
      return;
    }
    String ipAddressToCheck = smtpSession.getSmtpSocket().getRemoteAddress().hostAddress();
    DnsBlockListResponseCode response;
    try {
      response = DnsBlockListQuery
        .forIp(ipAddressToCheck)
        .query()
        .getFirst();
    } catch (IllegalStructure e) {
      // ipv6 address, we don't know how to filter
      return;
    }
    if (response.getBlocked()) {
      throw SmtpFiltering.getException("Ip " + ipAddressToCheck + " blacklisted (Reason: " + response.getDescription() + ")");
    }
  }

  private static final String BLACKLISTED_ENHANCED_CODE = "5.7.1.";

  /**
   * The response based on PostFix
   */
  private static SmtpException getException(String humanText) {
    return SmtpException.create(SmtpReplyCode.TRANSACTION_FAILED_554, BLACKLISTED_ENHANCED_CODE + " " + humanText)
      .setShouldQuit(true);
  }

  /**
   * A first party sender should be authenticated.
   */
  public static void checkFirstPartySenderShouldBeAuthenticated(SmtpSession session, BMailInternetAddress mailFromSender) throws SmtpException {
    if (session.isFirstPartyEmail(mailFromSender) && !session.isAuthenticatedOrIsLocalhost()) {
      throw SmtpException.create(SmtpReplyCode.TRANSACTION_FAILED_554, "A first party sender should be authenticated. Authenticate first.")
        .setShouldQuit(true);
    }
  }


  public static void ehloCheckHost(SmtpSession session, DnsName hostName) throws SmtpException {
    /**
     * <a href="https://datatracker.ietf.org/doc/html/rfc4408#section-2.1">...</a>
     * It is RECOMMENDED that SPF clients check the "HELO" identity by applying
     * the check_host() function (Section 4) to the "HELO" identity as the <sender>.
     */
    SmtpSpf.checkSenderHost(
      session,
      hostName,
      null
    );
  }


  public static void checkSender(SmtpSession smtpSession, BMailInternetAddress sender) throws SmtpException {
    /**
     * Check domain
     */
    SmtpFiltering.checkIfDomainIsNotBlocked(smtpSession, sender);
    /**
     * First Party should be authenticated
     */
    SmtpFiltering.checkFirstPartySenderShouldBeAuthenticated(smtpSession, sender);
    /**
     * https://datatracker.ietf.org/doc/html/rfc4408#section-2.2
     * SPF clients MUST check the "MAIL FROM" identity.  SPF clients check
     *    the "MAIL FROM" identity by applying the check_host() function to the
     *    "MAIL FROM" identity as the <sender>.
     */
    SmtpSpf.checkSenderHost(
      smtpSession,
      smtpSession.getTransactionState().getEhloClientHostName(),
      sender
    );

  }


}
