package net.bytle.vertx.auth;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.NotFoundException;
import net.bytle.exception.NullValueException;
import net.bytle.vertx.RoutingContextWrapper;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * A specialized object that adds context claims to a {@link AuthUser}
 * to make the difference between context claims and identity claims (ie {@link AuthUser})
 * <p>
 * * A JwtClaims object would be used in a validation link
 * * A AuthUser would be used as user in a session
 * Note that if you want to trace the token, you would use the JwtClaims Object
 * to add context on the creation
 */
public class AuthJwtClaims extends AuthUser {

  public static AuthJwtClaims createJwtClaimsFromJson(JsonObject jsonObject) {

    AuthJwtClaims jwtClaims = new AuthJwtClaims();
    jwtClaims.mergeClaims(jsonObject);
    return jwtClaims;

  }

  public static AuthJwtClaims createFromAuthUser(AuthUser authUser) {
    AuthJwtClaims jwtClaims = new AuthJwtClaims();
    jwtClaims.claims.mergeIn(authUser.claims);
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
    if(uriString==null){
      return null;
    }
    return URI.create(uriString);
  }

  public AuthJwtClaims setRedirectUri(URI redirectUri) {
    claims.put(AuthUserJwtClaims.CUSTOM_REDIRECT_URI.toString(), redirectUri.toString());
    return this;
  }

}
