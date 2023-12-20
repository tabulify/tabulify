package net.bytle.vertx.validator;

import io.vertx.core.Future;
import jakarta.mail.internet.AddressException;
import net.bytle.dns.DnsIllegalArgumentException;
import net.bytle.dns.DnsName;
import net.bytle.email.BMailInternetAddress;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.ValidationResult;

public class EmailAddressValidator {


  private final DomainValidator domainValidator;

  public EmailAddressValidator(TowerApp towerApp) {

    domainValidator = new DomainValidator(towerApp);
  }


  /**
   * @param email - the email to valid
   * @return {@link EmailAddressValidityReport}
   */
  public Future<EmailAddressValidityReport> validate(String email, boolean failEarly) {
    BMailInternetAddress mail;

    ValidationResult.Builder emailValidCheck = ValidationResult.builder("emailAddress");
    EmailAddressValidityReport.Builder emailValidityReport = EmailAddressValidityReport.builder(email);
    try {
      mail = BMailInternetAddress.of(email);
    } catch (AddressException e) {

      return Future.succeededFuture(
        emailValidityReport
          .addResult(emailValidCheck.fail("Email address is not valid"))
          .build());
    }
    emailValidityReport.addResult(emailValidCheck.succeed("Email address is valid"));

    /**
     * The domain to check
     */
    DnsName emailDomain;
    try {
      emailDomain = DnsName.create(mail.getDomain());
    } catch (DnsIllegalArgumentException e) {
      return Future.failedFuture(e);
    }

    /**
     * Domain Check
     */
    return domainValidator
      .validate(emailDomain, failEarly)
      .compose(res -> Future.succeededFuture(emailValidityReport.addResults(res).build()));

  }
}
