package net.bytle.vertx;

/**
 * The security auth name used in the spec file
 */
public class OpenApiSecurityNames {

  @SuppressWarnings("unused")
  public static final String BASIC_AUTH_SECURITY_SCHEME = "basicAuth";

  /**
   * Session Cookie
   */
  public static final String COOKIE_SECURITY_SCHEME = "cookieAuth";
  /**
   * Header X-API-KEY
   */
  public static final String APIKEY_AUTH_SECURITY_SCHEME = "apiKeyAuth";
  /**
   * JWT bearer header
   */
  public static final String BEARER_AUTH_SECURITY_SCHEME = "bearerAuth";
}
