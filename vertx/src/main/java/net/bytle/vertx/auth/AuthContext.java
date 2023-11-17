package net.bytle.vertx.auth;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.java.JavaEnvs;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.*;

import java.util.ArrayList;
import java.util.List;

/**
 * This class
 * * represents an authentication and can be passed around as event in a handler
 * * is a central entry point to authenticate the session in the whole application
 * * is a routing context extended with an auth user.
 * * implements a handler manager with {@link #next()}
 * <p>
 * It will authenticate the session
 * and redirect the user when all handlers have been called
 */
public class AuthContext {

  private final RoutingContext ctx;
  private final AuthUser authUser;
  private final AuthState authState;
  private final TowerApp towerApp;
  private List<Handler<AuthContext>> handlers = new ArrayList<>();
  private int handlerIndex = -1;

  public AuthContext(TowerApp towerApp, RoutingContext ctx, AuthUser user, AuthState authState) {
    this.ctx = ctx;
    this.authUser = user;
    this.authState = authState;
    this.towerApp = towerApp;
  }


  public AuthUser getAuthUser() {
    return this.authUser;
  }

  public RoutingContext getRoutingContext() {
    return this.ctx;
  }

  public AuthState getAuthState() {
    return this.authState;
  }

  /**
   * Call the next handler
   */
  public void next() {

    if (this.handlerIndex < this.handlers.size() - 1) {
      handlerIndex++;
      this.handlers.get(handlerIndex).handle(this);
    } else {
      this.nextLastHandlerSessionUpgrade();
    }

  }

  public AuthContext setHandlers(List<Handler<AuthContext>> oAuthSessionAuthenticationHandlers) {
    this.handlers = oAuthSessionAuthenticationHandlers;
    return this;
  }


  public enum RedirectionMethod {
    HTTP, // via HTTP where the uri is a defined uri or the redirect uri metadata
    NONE, // the client do a post request and navigate via Javascript to the origin page
  }

  private RedirectionMethod redirectVia = RedirectionMethod.HTTP;
  private UriEnhanced redirectUri;


  /**
   * @param uriToRedirect - the Uri to redirect
   * @return the object for chaining
   * We redirect to the frontend app to have a consistent design.
   * We don't validate the operation path for now other than with a visual / integration test.
   */
  public AuthContext redirectViaHttp(UriEnhanced uriToRedirect) {
    /**
     * We don't return an HTML template for consistency in the HTML app design
     * The user registers and gets the confirmation in the same design
     */
    this.redirectVia = RedirectionMethod.HTTP;
    this.redirectUri = uriToRedirect;
    return this;
  }

  /**
   * Redirect via the {@link AuthQueryProperty#REDIRECT_URI redirect uri parameter}
   * that is stored in the session (or in the auth state)
   */
  public AuthContext redirectViaHttpWithRedirectUriParameter() {
    this.redirectVia = RedirectionMethod.HTTP;
    /**
     * Try to get the original redirect uri parameters
     * <p>
     * The URI should be before authentication because the session may
     * be regenerated when the user is not the same
     * <p>
     * It can happen if the user is from another realm,
     * Say I log in to the combo portal as user of the realm Eraldy,
     * then I want to subscribe to a list from another realm,
     * I click on Oauth and a new user is created
     */
    try {
      redirectUri = this.getAndRemoveRedirectUri();
    } catch (IllegalStructure e) {

      String message = "An error prevents us to redirect you where you come from. The redirect uri was not valid.";
      if (JavaEnvs.IS_DEV) {
        message += e.getMessage();
      }
      // internal error, we don't throw
      VertxFailureHttpException.builder()
        .setMessage(message)
        .setStatus(HttpStatusEnum.INTERNAL_ERROR_500)
        .setName("Bad URL redirect")
        .setMimeToHtml()
        .setException(e)
        .buildWithContextFailingTerminal(ctx);
      return this;

    } catch (NotFoundException e) {

      // internal error, we don't throw
      VertxFailureHttpException.builder()
        .setMessage("An error prevents us to redirect you where you come from. We can't find where you come from (the redirect uri).")
        .setStatus(HttpStatusEnum.INTERNAL_ERROR_500)
        .setName("URL redirect was not found")
        .setMimeToHtml()
        .setException(e)
        .buildWithContextFailingTerminal(ctx);
      return this;

    }
    return this;
  }

  public AuthContext redirectViaClient() {
    this.redirectVia = RedirectionMethod.NONE;
    return this;
  }

  public void authenticateSession() {

    this.next();

  }

  /**
   * Sign-in
   */
  private void nextLastHandlerSessionUpgrade() {


    AuthUser sessionUser = null;
    User contextUser = ctx.user();
    if (contextUser != null) {

      JsonObject principal = contextUser.principal();
      sessionUser = AuthUser.createFromClaims(principal);

    }


    /**
     * If no user or not the same authenticated user
     */
    boolean sameUser = sessionUser != null && sessionUser.getSubject().equals(authUser.getSubject());
    if (!sameUser) {

      /**
       * Auth user check
       */
      if (authUser.getSubject() == null) {
        // The subject is the user id (for us, the user guid) and should be not null
        throw new InternalException("The authenticated user has no subject");
      }
      // not really needed to look up the user but nice to have
      if (authUser.getSubjectEmail() == null) {
        throw new InternalException("The authenticated user has no email");
      }
      // not really needed to look up the user but nice to have
      if (authUser.getAudience() == null) {
        throw new InternalException("The authenticated user has no audience");
      }

      contextUser = authUser.toVertxUser();
      ctx.setUser(contextUser);

      // the user has upgraded from unauthenticated to authenticated
      // session should be upgraded as recommended by owasp
      Session session = ctx.session();
      session.regenerateId();

      this.towerApp
        .getApexDomain()
        .getHttpServer()
        .getServer()
        .getTrackerAnalytics()
        .eventBuilder(AnalyticsEventName.SIGN_IN)
        .setUser(authUser)
        .setRoutingContext(this.getRoutingContext())
        .sendEventAsync();

    }

    /**
     * We don't upgrade the session to allow cross cookie session
     * even on trusted third party
     * (ie {@link EraldySessionHandler#upgradeSessionCookieToCrossCookie(RoutingContext)}
     * Why?
     * It's not done because it is client wise implemented.
     * The cookie is available on the Browser and is sent by this browser
     * every time a request is done where it comes from.
     * And there is no CSRF token to prevent CSRF.
     * <p>
     * A code authorization flows send a JWT
     */


    /**
     * Redirect
     */
    switch (this.redirectVia) {
      case HTTP:
        authenticationRedirect(this.redirectUri);
        break;
      case NONE:
        /**
         * The client does the redirect (Javascript)
         */
        break;
      default:
        throw new InternalException("The redirection method (" + this.redirectVia + ") is unknown");
    }
  }

  /**
   * Redirect without any cache
   *
   * @param redirectionUrl - the redirection URI
   */
  private void authenticationRedirect(UriEnhanced redirectionUrl) {
    if (this.redirectUri == null) {
      VertxFailureHttpException.builder()
        .setStatus(HttpStatusEnum.INTERNAL_ERROR_500)
        .setMessage("The redirect uri was not set with a redirect method")
        .buildWithContextFailingTerminal(ctx);
      return;
    }
    ctx.response()
      // disable all caching
      .putHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
      .putHeader("Pragma", "no-cache")
      .putHeader(HttpHeaders.EXPIRES, "0")
      // redirect (when there is no state, redirect to home)
      .putHeader(HttpHeaders.LOCATION, redirectionUrl.toUrl().toString())
      .setStatusCode(HttpStatusEnum.REDIRECT_SEE_OTHER_URI_303.getStatusCode())
      .end("Redirecting to " + redirectionUrl + ".");
  }

  /**
   * @return the redirect uri
   * @throws IllegalStructure  - the URI is not valid
   * @throws NotFoundException - the URI was not found (case when the user flow stops on the member website (ie list registration)
   */
  private UriEnhanced getAndRemoveRedirectUri() throws IllegalStructure, NotFoundException {

    Session session = ctx.session();

    /**
     * Redirection to the client
     * (with code and state for third-party client)
     */
    final UriEnhanced redirection;
    String sessionRedirectionUrl = session.remove(OAuthInternalSession.REDIRECT_URI_KEY);
    if (sessionRedirectionUrl == null) {
      throw new NotFoundException();
    }
    redirection = UriEnhanced.createFromString(sessionRedirectionUrl);


    /**
     * If the client is a third-party client,
     * we had the code and the state
     */
    if (!redirection.getApexWithoutPort().equals(EraldyDomain.get().getApexNameWithoutPort())) {
      String inState = session.get(OAuthInternalSession.STATE_KEY);
      if (inState == null) {
        throw new NotFoundException("The session state is null");
      }
      redirection.addQueryProperty(AuthQueryProperty.STATE.toString(), inState);
      String authCode = OAuthCodeManagement.createOrGet().createAuthorizationAndGetCode(sessionRedirectionUrl, authUser);
      redirection.addQueryProperty(AuthQueryProperty.CODE.toString(), authCode);
    }
    return redirection;

  }


}
