package net.bytle.tower.util;

/**
 * <a href="https://datatracker.ietf.org/doc/html/rfc7519#section-4">...JWT Claims</a>
 */
public enum JwtClaims {

  /**
   * The principal that issued the JWT
   * Application specific, a StringOrURI, OPTIONAL.
   * <a href="https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.1">Ref</a>
   */
  ISSUER("iss"),

  /**
   * The "sub" (subject) claim identifies the principal that is the subject of the JWT.
   * Application specific, a StringOrURI, OPTIONAL.
   * <a href="https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.2">Ref</a>
   * <p>
   * Other names
   * <ol>
   *  <li>{@code principal.username} - Usually for username/password or webauthn authentication</li>
   *  <li>{@code principal.userHandle} - Optional field for webauthn</li>
   *  <li>{@code attributes.idToken.sub} - For OpenID Connect ID Tokens</li>
   *  <li>{@code attributes.[rootClaim?]accessToken.sub} - For OpenID Connect/OAuth2 Access Tokens</li>
   * </ol>
   * <p></p>
   * The Guid
   */
  SUBJECT("sub"),
  /**
   * <a href="https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.6">Issued At Claim</a>
   */
  ISSUED_AT("iat"),

  /**
   * <a href="https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.4">Exp Claim</a>
   * NumericDate is a number in seconds since 1st Jan 1970 in UTC
   * <p>
   * Example: 5 over minutes
   * long actualTimestampInSec = System.currentTimeMillis() / 1000 + (5 * 60);
   * <p>
   * Note that {@link io.vertx.ext.auth.JWTOptions#setExpiresInMinutes(int)} do the same
   */
  EXPIRATION("exp"),

  /**
   * Audience:
   * - one string or URI
   * - or an array of string or URI
   * <p>
   * The interpretation of audience values is application specific and optional
   * <a href="https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.3">Aud Claim</a>
   */
  AUDIENCE("aud"),

  /**
   * Don't use before a time
   * <a href="https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.5">Ref</a>
   */
  NOT_BEFORE("nbf"),


  /**
   * Unique identifier for the JWT
   * <a href="https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.7">Ref</a>
   */
  JWT_ID("jti"),

  /**
   * Extra claims
   * We follow the naming convention of webauthn
   */
  CUSTOM_SUBJECT_HANDLE("userHandle"),
  CUSTOM_AUDIENCE_HANDLE("audienceHandle"),

  /**
   * The client IP of the request
   */
  CUSTOM_ORIGIN_CLIENT_IP("originClientIp"),

  /**
   * The email is a custom claim
   * `email` as property is used broadly.
   * Example at <a href="https://auth0.com/docs/get-started/apis/scopes/sample-use-cases-scopes-and-claims#add-custom-claims-to-a-token">OAuth</a>
   */
  CUSTOM_EMAIL("email"),

  /**
   * The URI referer for the request
   */
  CUSTOM_ORIGIN_REFERER("originReferer"),

  /**
   * The list guid for a list registration
   */
  CUSTOM_LIST_GUID("listGuid")
  ;

  private final String jwtKey;

  JwtClaims(String jwtKey) {
    this.jwtKey = jwtKey;
  }

  public String getJwtKey() {
    return jwtKey;
  }

  @Override
  public String toString() {
    return jwtKey;
  }
}
