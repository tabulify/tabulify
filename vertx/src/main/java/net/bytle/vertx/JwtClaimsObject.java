package net.bytle.vertx;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.*;
import net.bytle.type.time.Date;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * An object wrapper around a JWT {@link io.vertx.core.json.JsonObject}
 */
public class JwtClaimsObject {


  private static final String ERALDY_ISSUER_VALUE = "eraldy.com";
  private final JsonObject claims;

  public JwtClaimsObject(JsonObject claims) {
    this.claims = claims;
  }

  public static JwtClaimsObject createFromUser(UserClaims userClaims, RoutingContext routingContext) {
    JsonObject claims = new JsonObject();
    /**
     * Claims may be created for user registration, meaning that the user
     * does not exist in the database yet and has therefore no id
     */
    String subjectGuid = userClaims.getSubjectGuid();
    if (subjectGuid != null) {
      claims.put(JwtClaims.SUBJECT.toString(), subjectGuid);
    }
    claims.put(JwtClaims.AUDIENCE.toString(), userClaims.getAudienceRealmGuid());
    claims.put(JwtClaims.CUSTOM_EMAIL.toString(), userClaims.getEmail());
    claims.put(JwtClaims.CUSTOM_SUBJECT_HANDLE.toString(), userClaims.getHandle());
    claims.put(JwtClaims.CUSTOM_AUDIENCE_HANDLE.toString(), userClaims.getAudienceHandle());
    claims.put(JwtClaims.ISSUER.toString(), ERALDY_ISSUER_VALUE);

    /**
     * Routing Context may be null
     * when we create a JWT on test
     */
    if (routingContext != null) {
      RoutingContextWrapper routingContextWrapper = RoutingContextWrapper.createFrom(routingContext);

      String clientIp;
      try {
        clientIp = routingContextWrapper.getRealRemoteClientIp();
        claims.put(JwtClaims.CUSTOM_ORIGIN_CLIENT_IP.toString(), clientIp);
      } catch (NotFoundException ignored) {
      }
      URI optInUri;
      try {
        optInUri = routingContextWrapper.getReferer();
        claims.put(JwtClaims.CUSTOM_ORIGIN_REFERER.toString(), optInUri);
      } catch (NotFoundException ignored) {
      }
    }

    return new JwtClaimsObject(claims);

  }

  public static JwtClaimsObject createFromClaims(JsonObject jwtClaims) {
    return new JwtClaimsObject(jwtClaims);
  }

  public JsonObject toClaimsWithExpiration(Integer expirationInMinutes) {
    /**
     * This is the now used to test if the JWT is {@link io.vertx.ext.auth.User#expired(int)}
     */
    long now = (System.currentTimeMillis() / 1000);
    claims.put(JwtClaims.ISSUED_AT.toString(), now);
    claims.put(JwtClaims.EXPIRATION.toString(), now + expirationInMinutes * 60);
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
    return claims.getString(JwtClaims.ISSUER.toString());
  }

  public String getAudience() {
    return claims.getString(JwtClaims.AUDIENCE.toString());
  }

  public String getRealmHandle() {
    return claims.getString(JwtClaims.CUSTOM_AUDIENCE_HANDLE.toString());
  }

  public String getRealmGuid() {
    return getAudience();
  }

  public String getUserGuid() {
    return claims.getString(JwtClaims.SUBJECT.toString());
  }

  @SuppressWarnings("unused")
  public String getUserHandle() throws NullValueException {
    String userHandle = claims.getString(JwtClaims.CUSTOM_SUBJECT_HANDLE.toString());
    if (userHandle == null) {
      throw new NullValueException("No user handle");
    }
    return userHandle;
  }

  public String getEmail() {
    return claims.getString(JwtClaims.CUSTOM_EMAIL.getJwtKey());
  }

  @SuppressWarnings("unused")
  public String getOriginClientIp() throws NullValueException {
    String clientIp = claims.getString(JwtClaims.CUSTOM_ORIGIN_CLIENT_IP.getJwtKey());
    if (clientIp == null) {
      throw new NullValueException();
    }
    return clientIp;
  }

  public URI getOriginReferer() throws NullValueException, IllegalStructure {
    String referer = claims.getString(JwtClaims.CUSTOM_ORIGIN_REFERER.getJwtKey());
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
    String listGuid = claims.getString(JwtClaims.CUSTOM_LIST_GUID.toString());
    if (listGuid == null) {
      throw new NullValueException("No list guid");
    }
    return listGuid;
  }

  /**
   * @return the date
   */
  public Date getIssuedAt() {
    Long issuedAtInSec = claims.getLong(JwtClaims.ISSUED_AT.toString());
    if (issuedAtInSec == null) {
      throw new InternalException("The issued at should not be null");
    }
    return Date.createFromEpochSec(issuedAtInSec);
  }

  public JwtClaimsObject setListGuidClaim(String listGuid) {
    claims.put(JwtClaims.CUSTOM_LIST_GUID.toString(), listGuid);
    return this;
  }


}
