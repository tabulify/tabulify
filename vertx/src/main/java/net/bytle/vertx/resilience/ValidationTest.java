package net.bytle.vertx.resilience;

/**
 * The tests
 */
public enum ValidationTest {

  /**
   * Test if a home web page is legit
   */
  HOME_PAGE("homePage"),
  /**
   * Test if a web server exists and can be contacted
   * with proper SSL certificates
   */
  WEB_SERVER("webServer"),
  /**
   * Test if an MX record is present
   */
  MX_RECORD("mxRecord"),
  /**
   * Test if an A Record is present
   */
  A_RECORD("aRecord"),
  /**
   * Test if the search term (domain or ip) is in a block list
   */
  BLOCK_LIST("blockList"),
  /**
   * Test if the string is a valid email address
   */
  EMAIL_ADDRESS("emailAddress");
  private final String name;

  ValidationTest(String name) {
    this.name = name;
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

}
