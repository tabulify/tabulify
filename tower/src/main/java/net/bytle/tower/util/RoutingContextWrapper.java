package net.bytle.tower.util;

import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.ValidationException;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.auth.AuthRealmHandler;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.tower.eraldy.model.openapi.OrganizationUser;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.type.UriEnhanced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Wrap a {@link io.vertx.ext.web.RoutingContext} to extract information
 */
public class RoutingContextWrapper {

  public static final String CONTEXT_REROUTED_PATH = "REROUTE_ORIGINAL_REQUEST_PATH";
  protected static final Logger LOGGER = LoggerFactory.getLogger(RoutingContextWrapper.class);
  private static final String ROUTING_CONTEXT_WRAPPER_CTX_KEY = "RoutingContextWrapper";


  private final RoutingContext routingContext;
  private UriEnhanced requestUri;

  public RoutingContextWrapper(RoutingContext routingContext) {
    this.routingContext = routingContext;

  }

  public static RoutingContextWrapper createFrom(RoutingContext routingContext) {
    /**
     * Cache ... :) Could not stand to get the request uri multiple time as uri
     * {@link #getOriginalRequestAsUri()}
     */
    RoutingContextWrapper routingContextWrapper = routingContext.get(ROUTING_CONTEXT_WRAPPER_CTX_KEY);
    if (routingContextWrapper == null) {
      routingContextWrapper = new RoutingContextWrapper(routingContext);
      routingContext.put(ROUTING_CONTEXT_WRAPPER_CTX_KEY, routingContextWrapper);
    }
    return routingContextWrapper;
  }

  public String getRealRemoteClientIp() throws NotFoundException {

    return HttpRequestUtil.getRealRemoteClientIp(this.routingContext.request());
  }

  public URI getReferer() throws NotFoundException {

    String refererValue = this.routingContext.request().getHeader(HttpHeaders.REFERER);
    if (refererValue == null) {
      throw new NotFoundException("No referer");
    }
    try {
      return UriEnhanced.createFromString(refererValue).toUri();
    } catch (IllegalStructure e) {
      if (Env.IS_DEV) {
        throw ValidationException.create("The referer header is not a valid uri. Error:" + e.getMessage(), HttpHeaders.REFERER, refererValue);
      } else {
        String msg = "The referer header is not a valid uri (" + refererValue + "). Error:" + e.getMessage();
        LOGGER.warn(msg);
        throw new NotFoundException(msg);
      }
    }
  }

  /**
   * A {@link HttpStatus#REDIRECT 302} redirects the POST
   * The browser tries to perform the post on the redirected URI.
   * <p>
   * When you want to redirect to a GET page after a successful POST,
   * you should use a 303
   * <p>
   * Note that it does not work on Chrome
   *
   * @param redirectUri - the redirect URI
   */
  @SuppressWarnings("unused")
  public void seeOtherUriRedirect(String redirectUri) {
    this.routingContext.response()
      .setStatusCode(HttpStatus.REDIRECT_SEE_OTHER_URI)
      .putHeader(HttpHeaders.LOCATION, redirectUri)
      .putHeader(io.vertx.core.http.HttpHeaders.CONTENT_TYPE, "text/plain; charset=utf-8")
      .end("Redirecting to " + redirectUri + ".");
  }

  /**
   * @return the original request before {@link #reroute(String) rerouting} if any
   */
  public UriEnhanced getOriginalRequestAsUri() {
    if (this.requestUri != null) {
      return this.requestUri;
    }
    String url = this.routingContext.request().absoluteURI();
    try {
      this.requestUri = UriEnhanced.createFromString(url);
    } catch (IllegalStructure e) {
      // should not happen
      throw new InternalException("The request URI is not valid (" + url + ")", e);
    }
    String originalPath = this.getOriginalRequestPath();
    if (originalPath != null) {
      this.requestUri.setPath(originalPath);
    }
    return this.requestUri;
  }

  /**
   * @return the path of the original request before any rerouting
   */
  private String getOriginalRequestPath() {
    String pathBeforeRerouting = routingContext.get(CONTEXT_REROUTED_PATH, null);
    if (pathBeforeRerouting != null) {
      return pathBeforeRerouting;
    }
    return routingContext.request().path();
  }


  /**
   * A {@link RoutingContext#reroute(String) Reroute } should happen only once
   * This utility helps with it.
   */
  public boolean isReRouteOccurring() {
    String oldPath = routingContext.get(CONTEXT_REROUTED_PATH, null);
    return oldPath != null;
  }

  public void reroute(String reRouteString) {
    String oldPath = routingContext.request().path();
    routingContext
      .put(RoutingContextWrapper.CONTEXT_REROUTED_PATH, oldPath)
      .reroute(reRouteString);
  }

  public User getSignedInUser() throws NotFoundException {
    io.vertx.ext.auth.User user = this.routingContext.user();
    if(user==null){
      throw new NotFoundException();
    }
    return UsersUtil.vertxUserToEraldyUser(user);
  }

  public OrganizationUser getSignedInUserAsOrganizationUser() throws NotFoundException {
    io.vertx.ext.auth.User user = this.routingContext.user();
    if(user==null){
      throw new NotFoundException();
    }
    return UsersUtil.vertxUserToEraldyOrganizationUser(user);
  }

  public Vertx getVertx() {
    return this.routingContext.vertx();
  }

  public RoutingContext getRoutingContext() {
    return this.routingContext;
  }

  public Realm getAuthRealm() {
    return AuthRealmHandler.getFromRoutingContextKeyStore(this.routingContext);
  }
}
