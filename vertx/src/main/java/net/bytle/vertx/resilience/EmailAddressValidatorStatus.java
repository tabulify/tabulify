package net.bytle.vertx.resilience;

public enum EmailAddressValidatorStatus {

  /**
   * The email is legit
   */
  LEGIT(0, "Legit"),
  /**
   * A fatal error has occurred during
   * validation
   */
  FATAL_ERROR(1, "fatalError"),
  /**
   * The email address is invalid
   */
  EMAIL_ADDRESS_INVALID(2, "addressInvalid"),
  /**
   * The domain does not pass the test
   * (mx record, ...)
   */
  DOMAIN_SUSPICIOUS(3, "domainSuspicious"),
  /**
   * The domain is on an external blocking list
   */
  DOMAIN_BLOCKED(4, "domainBlocked"),
  /**
   * The email (ie domain) is soft banned
   * (meaning that the domain is banned for a period of time)
   * The domain didn't pass the validation tests (ie was suspicious)
   * The validation tests were not performed
   */
  SOFT_BAN(5, "softBan"),
  /**
   * The email (ie domain) is hard banned
   * (meaning that the domain is on our internal blocking list)
   */
  HARD_BAN(6, "hardBan");

  private final int statusCode;
  private final String statusName;

  EmailAddressValidatorStatus(int statusCode, String statusName) {
    this.statusCode = statusCode;
    this.statusName = statusName;
  }

  @Override
  public String toString() {
    return statusCode + " (" + statusName + ")";
  }
}
