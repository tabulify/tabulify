package net.bytle.vertx.auth;

/**
 * <a href="https://datatracker.ietf.org/doc/html/rfc7519#section-4">...JWT Claims</a>
 */
public enum AuthUserJwtClaims {

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
   * In Utc because we use it everywhere
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
   * For some application, it's the client ID of the application that should consume the token.
   * For us, this is the realm
   */
  AUDIENCE("aud"),

  /**
   * The app at the origin of the
   * login request
   */
  CUSTOM_APP_ID("app"),
  /**
   * The app handle at the origin of the request
   */
  CUSTOM_APP_HANDLE("appHandle"),

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
   * A subject handle is a unique descriptif name for the subject
   */
  CUSTOM_SUBJECT_HANDLE("subHandle"),

  /**
   * The client IP of the request
   */
  CUSTOM_ORIGIN_CLIENT_IP("originClientIp"),

  /**
   * The email is a custom claim
   * `email` as property is used broadly.
   * Example at <a href="https://auth0.com/docs/get-started/apis/scopes/sample-use-cases-scopes-and-claims#add-custom-claims-to-a-token">OAuth</a>
   */
  CUSTOM_SUBJECT_EMAIL("subEmail"),

  /**
   * The URI referer for the request
   */
  CUSTOM_ORIGIN_REFERER("originReferer"),

  /**
   * The list guid for a list registration
   */
  CUSTOM_LIST_GUID("listGuid"),

  /**
   * An audience handle is a unique descriptif name for the audience
   */
  CUSTOM_AUDIENCE_HANDLE("audienceHandle"),


  /**
   * The name of the subject
   */
  CUSTOM_SUBJECT_GIVEN_NAME("subGivenName"),

  /**
   * The subject bio
   */
  CUSTOM_SUBJECT_BIO("subBio"),
  /**
   * The subject blog
   */
  CUSTOM_SUBJECT_BLOG("subBlog"),
  /**
   * The subject location
   */
  CUSTOM_SUBJECT_LOCATION("subLocation"),
  /**
   * The subject avatar
   */
  CUSTOM_SUBJECT_AVATAR("subAvatar"),
  CUSTOM_SUBJECT_FAMILY_NAME("subFamilyName"),
  CUSTOM_SUBJECT_NAME("subName"),
  CUSTOM_ORG_GUID("orgGuid"),
  CUSTOM_ORG_HANDLE("orgHandle"),
  CUSTOM_REDIRECT_URI("redirectUri");
  private final String jwtKey;

  AuthUserJwtClaims(String jwtKey) {
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
