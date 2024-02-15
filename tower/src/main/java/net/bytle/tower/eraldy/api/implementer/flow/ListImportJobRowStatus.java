package net.bytle.tower.eraldy.api.implementer.flow;

public enum ListImportJobRowStatus {


  /**
   * The import completed without any error
   */
  COMPLETED(0, "complete"),
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
   * Data is invalid
   * (Example: an ip for the import that is not an ip)
   */
  DATA_INVALID(3, "dataInvalid"),
  /**
   * The domain does not pass the test
   * (mx record, ...)
   */
  DOMAIN_SUSPICIOUS(4, "domainSuspicious"),
  /**
   * The domain is on an external blocking list
   */
  DOMAIN_BLOCKED(5, "domainBlocked"),
  /**
   * The email (ie domain) is soft banned
   * (meaning that the domain is banned for a period of time)
   * The domain didn't pass the validation tests (ie was suspicious)
   * The validation tests were not performed
   */
  SOFT_BAN(6, "softBan"),
  /**
   * The email (ie domain) is hard banned
   * (meaning that the domain is on our internal blocking list)
   */
  HARD_BAN(7, "hardBan"),
  /**
   * The domain is in the grey area
   * meaning that the email cannot be imported
   * without any human email validation
   * (163.com mostly)
   */
  GREY_BAN(8, "greyBan");

  private final int statusCode;
  private final String statusName;

  ListImportJobRowStatus(int statusCode, String statusName) {
    this.statusCode = statusCode;
    this.statusName = statusName;
  }

  @Override
  public String toString() {
    return statusCode + " (" + statusName + ")";
  }


  public int getStatusCode() {
    return this.statusCode;
  }


}
