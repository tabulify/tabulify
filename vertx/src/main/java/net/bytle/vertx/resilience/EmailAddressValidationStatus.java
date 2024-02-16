package net.bytle.vertx.resilience;

public enum EmailAddressValidationStatus {

  /**
   * The email address is legit
   */
  LEGIT(0, "legit", 0),
  /**
   * A fatal error has occurred during
   * validation
   */
  FATAL_ERROR(1, "fatalError", 999),
  /**
   * The email address is invalid
   */
  EMAIL_ADDRESS_INVALID(2, "addressInvalid", 900),

  /**
   * The domain does not pass the test
   * (mx record, ...)
   */
  DOMAIN_SUSPICIOUS(3, "domainSuspicious", 700),
  /**
   * The domain is on an external blocking list
   */
  DOMAIN_BLOCKED(4, "domainBlocked", 800),
  /**
   * The email (ie domain) is soft banned
   * (meaning that the domain is banned for a period of time)
   * The domain didn't pass the validation tests (ie was suspicious)
   * The validation tests were not performed
   */
  SOFT_BAN(5, "softBan", 700),
  /**
   * The email (ie domain) is hard banned
   * (meaning that the domain is on our internal blocking list)
   */
  HARD_BAN(6, "hardBan", 500),
  /**
   * The domain is valid but may be used to create
   * spam email. More validation is needed
   */
  GREY_BAN(7, "greyBan", 100),

  /**
   * A valid email for a mailing list should be an apex domain
   * The chance of the domain being on a sub apex domain is nihil
   */
  NOT_AN_APEX_DOMAIN(8, "apexDomain", 100);

  private final int statusCode;
  private final String statusName;
  /**
   * If the validator has multiple status,
   * the status with the higher order wins
   */
  private final int orderOfPrecedence;

  EmailAddressValidationStatus(int statusCode, String statusName, int orderOfPrecedence) {
    this.statusCode = statusCode;
    this.statusName = statusName;
    this.orderOfPrecedence = orderOfPrecedence;
  }

  @Override
  public String toString() {
    return statusCode + " (" + statusName + ")";
  }

  public int getOrderOfPrecedence() {
    return this.orderOfPrecedence;
  }

  public int getStatusCode() {
    return this.statusCode;
  }

}
