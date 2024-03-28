package net.bytle.vertx.auth;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.*;
import net.bytle.type.time.Timestamp;
import net.bytle.vertx.DateTimeUtil;
import net.bytle.vertx.RoutingContextWrapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZoneOffset;

/**
 * A JWT claims is an object that adds request/context claims
 * that are not related to the identity as an AuthUser do.
 * <p>
 * * A JwtClaims object is used in a validation link
 * * A AuthUser would be used as user in a session
 * <p>
 * It's a specialized auth object that adds request context claims to a {@link AuthUser}
 * <p>
 * If we allow a user to have an expiration date, we would need to merge them
 */
public class AuthJwtClaims {

  private static final String ERALDY_ISSUER_VALUE = "eraldy.com";
  private final JsonObject claims = new JsonObject();

  public AuthJwtClaims() {
    claims.put(AuthUserJwtClaims.ISSUER.toString(), ERALDY_ISSUER_VALUE);
  }

  public static AuthJwtClaims createJwtClaimsFromJson(JsonObject jsonObject) {

    AuthJwtClaims jwtClaims = new AuthJwtClaims();
    jwtClaims.claims.mergeIn(jsonObject);
    return jwtClaims;

  }

  public static AuthJwtClaims createFromAuthUser(AuthUser authUser) {
    AuthJwtClaims jwtClaims = new AuthJwtClaims();
    jwtClaims.claims.mergeIn(authUser.getClaims());
    return jwtClaims;
  }

  public static AuthJwtClaims createEmptyClaims() {
    return new AuthJwtClaims();
  }

  public AuthJwtClaims addRequestClaims(RoutingContext routingContext) {


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

  public AuthJwtClaims setListGuid(String listGuid) {
    claims.put(AuthUserJwtClaims.CUSTOM_LIST_GUID.toString(), listGuid);
    return this;
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
   * @param app - the app id
   */
  public AuthJwtClaims setAppGuid(String app) {
    claims.put(AuthUserJwtClaims.CUSTOM_APP_ID.toString(), app);
    return this;
  }

  /**
   * The app id
   */
  public String getAppGuid() {
    return claims.getString(AuthUserJwtClaims.CUSTOM_APP_ID.toString());
  }

  /**
   * @param appHandle - the app handle
   */
  public AuthJwtClaims setAppHandle(String appHandle) {
    claims.put(AuthUserJwtClaims.CUSTOM_APP_HANDLE.toString(), appHandle);
    return this;
  }

  public String getAppHandle() {
    return claims.getString(AuthUserJwtClaims.CUSTOM_APP_HANDLE.toString());
  }


  public URI getRedirectUri() {
    String uriString = claims.getString(AuthUserJwtClaims.CUSTOM_REDIRECT_URI.toString());
    if (uriString == null) {
      return null;
    }
    return URI.create(uriString);
  }

  public AuthJwtClaims setRedirectUri(URI redirectUri) {
    claims.put(AuthUserJwtClaims.CUSTOM_REDIRECT_URI.toString(), redirectUri.toString());
    return this;
  }

  public JsonObject toClaimsWithExpiration(Integer expirationInMinutes) {
    /**
     * Note Time is everywhere in UTC.
     * It will not work with {@link io.vertx.ext.auth.User#expired(int)},
     * because they use local system time
     * ie long now = System.currentTimeMillis() / 1000;
     */
    long now = DateTimeUtil.getNowInUtc().toEpochSecond(ZoneOffset.UTC);
    claims.put(AuthUserJwtClaims.ISSUED_AT.toString(), now);
    claims.put(AuthUserJwtClaims.EXPIRATION.toString(), now + expirationInMinutes * 60);

    return claims;
  }

  /**
   * @return the issued date in UTC
   */
  public Timestamp getIssuedAt() {
    Long issuedAtInSec = claims.getLong(AuthUserJwtClaims.ISSUED_AT.toString());
    if (issuedAtInSec == null) {
      throw new InternalException("The issued at should not be null");
    }
    return Timestamp.createFromEpochSec(issuedAtInSec);
  }

  private String getIssuer() {
    return claims.getString(AuthUserJwtClaims.ISSUER.toString());
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

  public AuthUser toAuthUser() {
    return AuthUser.createUserFromJsonClaims(this.claims);
  }


}
