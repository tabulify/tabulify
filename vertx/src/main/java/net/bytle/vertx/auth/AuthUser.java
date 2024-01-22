package net.bytle.vertx.auth;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.*;
import net.bytle.type.Casts;
import net.bytle.type.time.Timestamp;
import net.bytle.vertx.RoutingContextWrapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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

  public AuthUser addRequestClaims(RoutingContext routingContext) {


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
    } catch (NotFoundException | IllegalStructure ignored) {
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


  public String getSubject() {
    return claims.getString(AuthUserJwtClaims.SUBJECT.toString());
  }

  @SuppressWarnings("unused")
  public String getSubjectHandle() throws NullValueException {
    String userHandle = claims.getString(AuthUserJwtClaims.CUSTOM_SUBJECT_HANDLE.toString());
    if (userHandle == null) {
      throw new NullValueException("No subject handle");
    }
    return userHandle;
  }

  public String getSubjectEmail() {
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
  public Timestamp  getIssuedAt() {
    Long issuedAtInSec = claims.getLong(AuthUserJwtClaims.ISSUED_AT.toString());
    if (issuedAtInSec == null) {
      throw new InternalException("The issued at should not be null");
    }
    return Timestamp.createFromEpochSec(issuedAtInSec);
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

  /**
   * @param client - the client id (for us, the app guid)
   */
  public AuthUser setClient(String client) {
    claims.put(AuthUserJwtClaims.CUSTOM_CLIENT_ID.toString(), client);
    return this;
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

  public void setSubjectGivenName(String subjectGivenName) {
    claims.put(AuthUserJwtClaims.CUSTOM_SUBJECT_GIVEN_NAME.toString(), subjectGivenName);
  }

  public void setSubjectBio(String bio) {
    claims.put(AuthUserJwtClaims.CUSTOM_SUBJECT_BIO.toString(), bio);
  }

  public void setSubjectBlog(URI blogUri) {
    claims.put(AuthUserJwtClaims.CUSTOM_SUBJECT_BLOG.toString(), blogUri);
  }

  public void setSubjectLocation(String location) {
    claims.put(AuthUserJwtClaims.CUSTOM_SUBJECT_LOCATION.toString(), location);
  }

  public void setSubjectAvatar(URI avatarUri) {
    claims.put(AuthUserJwtClaims.CUSTOM_SUBJECT_AVATAR.toString(), avatarUri);
  }

  public void setSubjectFamilyName(String familyName) {
    claims.put(AuthUserJwtClaims.CUSTOM_SUBJECT_FAMILY_NAME.toString(), familyName);
  }

  public String getSubjectGivenName() {
    return claims.getString(AuthUserJwtClaims.CUSTOM_SUBJECT_GIVEN_NAME.toString());
  }

  public String getSubjectBio() {
    return claims.getString(AuthUserJwtClaims.CUSTOM_SUBJECT_BIO.toString());
  }

  public String getSubjectFamilyName() {
    return claims.getString(AuthUserJwtClaims.CUSTOM_SUBJECT_FAMILY_NAME.toString());
  }

  public String getSubjectLocation() {
    return claims.getString(AuthUserJwtClaims.CUSTOM_SUBJECT_LOCATION.toString());
  }

  public URI getSubjectBlog() {
    try {
      String blogUrl = claims.getString(AuthUserJwtClaims.CUSTOM_SUBJECT_BLOG.toString());
      if (blogUrl == null) {
        return null;
      }
      return new URI(blogUrl);
    } catch (URISyntaxException e) {
      throw new InternalException("Should not happen because the setter is an URI", e);
    }
  }

  public URI getSubjectAvatar() {
    try {
      String avatarUrl = claims.getString(AuthUserJwtClaims.CUSTOM_SUBJECT_AVATAR.toString());
      if (avatarUrl == null) {
        return null;
      }
      return new URI(avatarUrl);
    } catch (URISyntaxException e) {
      throw new InternalException("Avatar should be an URI because the setter is an URI", e);
    }
  }

  public String getAudienceHandle() {
    return claims.getString(AuthUserJwtClaims.CUSTOM_AUDIENCE_HANDLE.toString());
  }

  public void setGroup(String group) {
    claims.put(AuthUserJwtClaims.CUSTOM_GROUP.toString(), group);
  }

  public String getGroup() {
    return claims.getString(AuthUserJwtClaims.CUSTOM_GROUP.toString());
  }

  public <T> Set<T> getSet(String key, Class<T> clazz) {
    JsonArray jsonArray = claims.getJsonArray(key);
    if (jsonArray == null) {
      return new HashSet<>();
    }
    return jsonArray.stream()
      .map(e -> {
        try {
          return Casts.cast(e, clazz);
        } catch (CastException ex) {
          throw new InternalException("The value (" + e + ") of the claims key (" + key + ") is not a " + clazz, ex);
        }
      })
      .collect(Collectors.toSet());
  }

  @Override
  public String toString() {

    String toString = "";
    String subjectEmail = this.getSubjectEmail();
    if (subjectEmail != null) {
      toString = subjectEmail;
    }
    String subject = this.getSubject();
    if (subject != null) {
      toString = ", " + subject;
    }
    return toString;

  }

  public void put(String key, Object obj) {
    claims.put(key, obj);
  }

  /**
   * The client id (for us the app guid for now)
   */
  public String getClient() {
    return claims.getString(AuthUserJwtClaims.CUSTOM_CLIENT_ID.toString());
  }

}
