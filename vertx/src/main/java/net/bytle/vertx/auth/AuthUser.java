package net.bytle.vertx.auth;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.*;
import net.bytle.type.time.Date;
import net.bytle.vertx.RoutingContextWrapper;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * A JWT claims object that represents a auth user with extra environment claims
 * This is a user object in a AUTH scope
 * <p>
 * Claims may be created for user registration, meaning that the user
 * does not exist in the database yet and has therefore no id
 */
public class AuthUser {


  private static final String ERALDY_ISSUER_VALUE = "eraldy.com";
  private final JsonObject claims;

  public AuthUser() {
    this.claims = new JsonObject();
    claims.put(AuthUserJwtClaims.ISSUER.toString(), ERALDY_ISSUER_VALUE);
  }

  public AuthUser addRoutingClaims(RoutingContext routingContext) {


    RoutingContextWrapper routingContextWrapper = RoutingContextWrapper.createFrom(routingContext);
    String clientIp;
    try {
      clientIp = routingContextWrapper.getRealRemoteClientIp();
      claims.put(AuthUserJwtClaims.CUSTOM_ORIGIN_CLIENT_IP.toString(), clientIp);
    } catch (NotFoundException ignored) {
    }
    URI optInUri;
    try {
      optInUri = routingContextWrapper.getReferer();
      claims.put(AuthUserJwtClaims.CUSTOM_ORIGIN_REFERER.toString(), optInUri);
    } catch (NotFoundException ignored) {
    }

    return this;

  }

  public static AuthUser createFromClaims(JsonObject jwtClaims) {

    AuthUser authUser = new AuthUser();
    authUser.mergeClaims(jwtClaims);
    return authUser;

  }

  private void mergeClaims(JsonObject jwtClaims) {
    this.claims.mergeIn(jwtClaims);
  }

  public JsonObject toClaimsWithExpiration(Integer expirationInMinutes) {
    /**
     * This is the now used to test if the JWT is {@link io.vertx.ext.auth.User#expired(int)}
     */
    long now = (System.currentTimeMillis() / 1000);
    claims.put(AuthUserJwtClaims.ISSUED_AT.toString(), now);
    claims.put(AuthUserJwtClaims.EXPIRATION.toString(), now + expirationInMinutes * 60);
    return claims;
  }

  public void checkValidityAndExpiration() throws IllegalStructure, ExpiredException {

    String issuer = this.getIssuer();
    if (issuer == null || !issuer.equals(ERALDY_ISSUER_VALUE)) {
      throw new IllegalStructure("Invalid JWT issuer");
    }

    if (io.vertx.ext.auth.User.create(this.claims).expired()) {
      throw new ExpiredException();
    }

  }

  private String getIssuer() {
    return claims.getString(AuthUserJwtClaims.ISSUER.toString());
  }

  public String getAudience() {
    return claims.getString(AuthUserJwtClaims.AUDIENCE.toString());
  }

  public String getRealmIdentifier() {
    return getAudience();
  }


  public String getUserGuid() {
    return claims.getString(AuthUserJwtClaims.SUBJECT.toString());
  }

  @SuppressWarnings("unused")
  public String getUserHandle() throws NullValueException {
    String userHandle = claims.getString(AuthUserJwtClaims.CUSTOM_SUBJECT_HANDLE.toString());
    if (userHandle == null) {
      throw new NullValueException("No user handle");
    }
    return userHandle;
  }

  public String getEmail() {
    return claims.getString(AuthUserJwtClaims.CUSTOM_SUBJECT_EMAIL.getJwtKey());
  }

  @SuppressWarnings("unused")
  public String getOriginClientIp() throws NullValueException {
    String clientIp = claims.getString(AuthUserJwtClaims.CUSTOM_ORIGIN_CLIENT_IP.getJwtKey());
    if (clientIp == null) {
      throw new NullValueException();
    }
    return clientIp;
  }

  public URI getOriginReferer() throws NullValueException, IllegalStructure {
    String referer = claims.getString(AuthUserJwtClaims.CUSTOM_ORIGIN_REFERER.getJwtKey());
    if (referer == null) {
      throw new NullValueException();
    }
    try {
      return new URI(referer);
    } catch (URISyntaxException e) {
      throw new IllegalStructure(referer + " is not a valid uri");
    }
  }

  public String getListGuid() throws NullValueException {
    String listGuid = claims.getString(AuthUserJwtClaims.CUSTOM_LIST_GUID.toString());
    if (listGuid == null) {
      throw new NullValueException("No list guid");
    }
    return listGuid;
  }

  /**
   * @return the date
   */
  public Date getIssuedAt() {
    Long issuedAtInSec = claims.getLong(AuthUserJwtClaims.ISSUED_AT.toString());
    if (issuedAtInSec == null) {
      throw new InternalException("The issued at should not be null");
    }
    return Date.createFromEpochSec(issuedAtInSec);
  }

  public AuthUser setListGuidClaim(String listGuid) {
    claims.put(AuthUserJwtClaims.CUSTOM_LIST_GUID.toString(), listGuid);
    return this;
  }

  /**
   * @param subject - a subject/user identifier for the audience
   */
  public void setSubject(String subject) {
    claims.put(AuthUserJwtClaims.SUBJECT.toString(), subject);
  }

  /**
   * @param subjectHandle - a handle is a unique descriptif name for the subject
   */
  public void setSubjectHandle(String subjectHandle) {
    claims.put(AuthUserJwtClaims.CUSTOM_SUBJECT_HANDLE.toString(), subjectHandle);
  }

  /**
   * @param email - the email of the subject
   */
  public void setSubjectEmail(String email) {
    claims.put(AuthUserJwtClaims.CUSTOM_SUBJECT_EMAIL.toString(), email);
  }

  /**
   * @param audience - a namespace where the subject is unique (an application, a realm, ...)
   */
  public void setAudience(String audience) {
    claims.put(AuthUserJwtClaims.AUDIENCE.toString(), audience);
  }

  public void setAudienceHandle(String audienceHandle) {
    claims.put(AuthUserJwtClaims.CUSTOM_AUDIENCE_HANDLE.toString(), audienceHandle);
  }

  public User toVertxUser() {

    /**
     * Not completely finish
     * A second argument attributes can be provided to provide extra metadata for later usage.
     * One example are the following attributes:
     * * exp - Expires at in seconds.
     * * iat - Issued at in seconds.
     * * nbf - Not before in seconds.
     * * leeway - clock drift leeway in seconds.
     * <p>
     * The first 3 control how the expired method will compute the expiration of the user,
     * the last can be used to allow clock drifting compensation while computing the expiration time.
     */
    return User.create(claims);

  }

}
