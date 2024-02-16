package net.bytle.vertx.resilience;

import io.vertx.core.Future;
import net.bytle.type.DnsName;
import net.bytle.type.EmailAddress;
import net.bytle.type.EmailCastException;
import net.bytle.vertx.TowerApp;

public class EmailAddressValidator {


  private final DomainValidator domainValidator;


  public EmailAddressValidator(TowerApp towerApp) {

    domainValidator = new DomainValidator(towerApp);
  }


  /**
   * @param email - the email to valid
   * @return {@link EmailAddressValidatorReport}
   */
  public Future<EmailAddressValidatorReport> validate(String email, boolean failEarly) {


    ValidationTestResult.Builder emailValidCheck = ValidationTest.EMAIL_ADDRESS.createResultBuilder();
    EmailAddressValidatorReport.Builder emailValidityReport = EmailAddressValidatorReport.builder(email);
    EmailAddress internetAddress;
    try {
      internetAddress = EmailAddress.of(email);
      emailValidityReport.setEmailInternetAddress(internetAddress);
    } catch (EmailCastException e) {
      return Future.succeededFuture(
        emailValidityReport
          .addResult(emailValidCheck.setMessage("Email address is not valid (" + e.getMessage() + ")").fail())
          .build()
      );
    }
    emailValidityReport.addResult(emailValidCheck.setMessage("Email address is valid").succeed());

    /**
     * The domain to check
     */
    DnsName emailDomain = internetAddress.getDomainName();

    /**
     * Domain Check
     */
    return domainValidator
      .validate(emailDomain, failEarly)
      .compose(res -> Future.succeededFuture(emailValidityReport.addResults(res.getResults()).build()));

  }
}
