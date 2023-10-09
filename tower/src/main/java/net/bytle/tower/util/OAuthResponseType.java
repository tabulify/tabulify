package net.bytle.tower.util;

/**
 * The response type supported by our OAuth server
 */
public enum OAuthResponseType {

  /**
   * The classic code OAUTH authorization code
   */
  CODE("code"),
  /**
   * Internal, the returned authentication
   * material is a session cookie (used for the combostrap domain)
   */
  SESSION_COOKIE("cookie"),
  ;

  private final String value;

  OAuthResponseType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

}
