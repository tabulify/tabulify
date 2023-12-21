package net.bytle.vertx.resilience;

/**
 * The tests
 */
public enum ValidationTest {

  /**
   * Test if a home web page is legit
   */
  HOME_PAGE("homePage", ValidationStatus.DOMAIN_SUSPICIOUS),
  /**
   * Test if a web server exists and can be contacted
   * with proper SSL certificates
   */
  WEB_SERVER("webServer", ValidationStatus.DOMAIN_SUSPICIOUS),
  /**
   * Test if an MX record is present
   */
  MX_RECORD("mxRecord", ValidationStatus.DOMAIN_SUSPICIOUS),
  /**
   * Test if an A Record is present
   */
  A_RECORD("aRecord", ValidationStatus.DOMAIN_SUSPICIOUS),
  /**
   * Test if the search term (domain or ip) is in a block list
   */
  BLOCK_LIST("blockList", ValidationStatus.DOMAIN_BLOCKED),
  /**
   * Test if the string is a valid email address
   */
  EMAIL_ADDRESS("emailAddress", ValidationStatus.EMAIL_ADDRESS_INVALID);
  private final String name;
  private final ValidationStatus validationStatus;

  ValidationTest(String name, ValidationStatus domainSuspicious) {
    this.name = name;
    this.validationStatus = domainSuspicious;
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

  public ValidationStatus getValidationType() {
    return this.validationStatus;
  }
}
