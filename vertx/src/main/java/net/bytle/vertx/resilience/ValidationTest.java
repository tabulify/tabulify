package net.bytle.vertx.resilience;

/**
 * The tests
 */
public enum ValidationTest {

  /**
   * Test if a home web page is legit
   */
  HOME_PAGE("homePage", EmailAddressValidationStatus.DOMAIN_SUSPICIOUS),
  /**
   * Test if a web server exists and can be contacted
   * with proper SSL certificates
   */
  WEB_SERVER("webServer", EmailAddressValidationStatus.DOMAIN_SUSPICIOUS),
  /**
   * Test if an MX record is present
   */
  MX_RECORD("mxRecord", EmailAddressValidationStatus.DOMAIN_SUSPICIOUS),
  /**
   * Test if an A Record is present
   */
  A_RECORD("aRecord", EmailAddressValidationStatus.DOMAIN_SUSPICIOUS),
  /**
   * Test if the search term (domain or ip) is in a block list
   */
  BLOCK_LIST("blockList", EmailAddressValidationStatus.DOMAIN_BLOCKED),
  /**
   * Test if the string is a valid email address
   */
  EMAIL_ADDRESS("emailAddress", EmailAddressValidationStatus.EMAIL_ADDRESS_INVALID),
  /**
   * Test if the domain is on a while list
   */
  WHITE_LIST("whiteList", EmailAddressValidationStatus.LEGIT);
  private final String name;
  private final EmailAddressValidationStatus emailAddressValidationStatus;

  ValidationTest(String name, EmailAddressValidationStatus domainSuspicious) {
    this.name = name;
    this.emailAddressValidationStatus = domainSuspicious;
  }

  public ValidationTestResult.Builder createResultBuilder() {
    return new ValidationTestResult.Builder(this);
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }

  public EmailAddressValidationStatus getValidationType() {
    return this.emailAddressValidationStatus;
  }
}
