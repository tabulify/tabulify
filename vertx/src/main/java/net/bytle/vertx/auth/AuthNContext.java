package net.bytle.vertx.auth;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.NotFoundException;
import net.bytle.java.JavaEnvs;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.analytics.event.SignInEvent;
import net.bytle.vertx.analytics.model.AnalyticsEventClient;
import net.bytle.vertx.flow.WebFlow;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
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
public class AuthNContext {

  private final RoutingContext ctx;
  private final AuthJwtClaims jwtClaims;
  private final WebFlow flow;
  private AuthUser authUser;
  private final OAuthState oAuthState;
  private final AuthNContextManager authNContextManager;

  private int handlerIndex = -1;

  /**
   * @param authNContextManager - the authN manager with the configuration
   * @param webFlow             - the flow that logs the user in
   * @param ctx                 - the context
   * @param user                - the user to set on the session
   * @param oAuthState          - the auth state from an external oauth endpoint
   * @param jwtClaims           - the context claims from a link on an email or for a direct login
   */
  protected AuthNContext(AuthNContextManager authNContextManager, WebFlow webFlow, RoutingContext ctx, AuthUser user, OAuthState oAuthState, AuthJwtClaims jwtClaims) {
    this.ctx = ctx;
    this.authUser = user;
    this.oAuthState = oAuthState;
    this.authNContextManager = authNContextManager;
    this.jwtClaims = jwtClaims;
    this.flow = webFlow;
  }


  public AuthUser getAuthUser() {
    return this.authUser;
  }

  public RoutingContext getRoutingContext() {
    return this.ctx;
  }

  public OAuthState getOAuthState() {
    return this.oAuthState;
  }

  /**
   * Call the next handler
   */
  public void next() {
    List<Handler<AuthNContext>> handlers = this.authNContextManager.getHandlers();
    if (this.handlerIndex < handlers.size() - 1) {
      handlerIndex++;
      handlers.get(handlerIndex).handle(this);
    } else {
      this.nextLastHandlerSessionUpgrade();
    }

  }


  public void setAuthUser(AuthUser authUser) {
    this.authUser = authUser;
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
  public AuthNContext redirectViaHttp(UriEnhanced uriToRedirect) {
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
   * that is stored in the session or in the state (auth state and jwt claims)
   */
  public AuthNContext redirectViaHttpToAuthRedirectUri() {
    this.redirectVia = RedirectionMethod.HTTP;
    /**
     * We don't set the redirectUri with {@link #getRedirectUriFromSessionOrClaims()}
     * because the redirect uri may by changed by the auth handlers.
     */
    return this;
  }

  public AuthNContext redirectViaClient() {
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
      sessionUser = AuthUser.createUserFromJsonClaims(principal);

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
        TowerFailureException.builder()
          .setMessage("The authenticated user has no subject")
          .buildWithContextFailingTerminal(ctx);
        return;
      }
      // not really needed to look up the user but nice to have
      if (authUser.getSubjectEmail() == null) {
        TowerFailureException.builder()
          .setMessage("The authenticated user has no email")
          .buildWithContextFailingTerminal(ctx);
        return;
      }
      // not really needed to look up the user but nice to have
      if (authUser.getAudience() == null) {
        TowerFailureException.builder()
          .setMessage("The authenticated user has no audience")
          .buildWithContextFailingTerminal(ctx);
        return;
      }

      /**
       * the user has upgraded from unauthenticated to authenticated
       *  session should be upgraded as recommended by owasp
       */
      Session session = ctx.session();
      if (session != null) {
        session.regenerateId();
        String realmSessionKey = this.authNContextManager.getRealmGuidSessionKey();
        if (realmSessionKey != null) {
          String realmSessionValue = session.get(realmSessionKey);
          if (realmSessionValue == null) {
            TowerFailureException.builder()
              .setMessage("The realm session key (" + realmSessionKey + ") does not return any value")
              .buildWithContextFailingTerminal(ctx);
            return;
          }
          String realmUserValue = authUser.getRealmGuid();
          if (!realmSessionValue.equals(realmUserValue)) {
            TowerFailureException.builder()
              .setMessage("The realm of the authenticated user (" + realmUserValue + ") and the realm of the session (" + realmSessionValue + ") differs.")
              .buildWithContextFailingTerminal(ctx);
            return;
          }
        }
      }

      contextUser = authUser.getVertxUser();
      ctx.setUser(contextUser);

      SignInEvent signInEvent;
      try {
        signInEvent = getSignInEvent();
      } catch (NotFoundException e) {
        TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.INTERNAL_ERROR_500)
          .setCauseException(e)
          .setMessage(e.getMessage())
          .buildWithContextFailingTerminal(ctx);
        return;
      }

      /**
       * You need to log out to come to this point in the code.
       * If you log in while already logged in,
       * we don't upgrade the session.
       */
      this.flow
        .getApp()
        .getHttpServer()
        .getServer()
        .getTrackerAnalytics()
        .eventBuilder(signInEvent)
        .setAuthUser(authUser)
        .setRoutingContext(this.getRoutingContext())
        .processEvent();

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
        authenticationRedirect();
        break;
      case NONE:
        /**
         * The client does the redirect (Javascript)
         */
        break;
      default:
        TowerFailureException
          .builder()
          .setMessage("The redirection method (" + this.redirectVia + ") is unknown")
          .buildWithContextFailingTerminal(ctx);
    }
  }

  @NotNull
  private SignInEvent getSignInEvent() throws NotFoundException {
    SignInEvent signInEvent = new SignInEvent();
    signInEvent.getRequest().setFlowGuid(this.flow.getFlowType().getId().toString());
    signInEvent.getRequest().setFlowHandle(this.flow.getFlowType().toString());
    String appIdentifier = this.oAuthState.getAppGuid();
    if (appIdentifier == null) {
      appIdentifier = this.jwtClaims.getAppGuid();
      if (appIdentifier == null && JavaEnvs.IS_DEV) {
        throw new NotFoundException("The app Identifier was not found (in the AuthUser or AuthState) for the Sign-in with the flow (" + this.flow.getClass().getSimpleName() + ")");
      }
    }
    /**
     * App data
     */
    AnalyticsEventClient app = signInEvent.getApp();
    app.setAppGuid(appIdentifier);
    String appHandle = this.oAuthState.getAppHandle();
    if (appHandle == null) {
      appHandle = this.jwtClaims.getAppHandle();
    }
    app.setAppHandle(appHandle);
    String realmIdentifier = this.oAuthState.getRealmGuid();
    if (realmIdentifier == null) {
      realmIdentifier = this.authUser.getRealmGuid();
    }
    app.setAppRealmGuid(realmIdentifier);

    String realmHandle = this.oAuthState.getRealmHandle();
    if (realmHandle == null) {
      realmHandle = this.authUser.getRealmHandle();
    }
    app.setAppRealmHandle(realmHandle);

    String orgIdentifier = this.oAuthState.getOwnerOrganisationGuid();
    if (orgIdentifier == null) {
      orgIdentifier = this.authUser.getAudienceOwnerOrganizationGuid();
    }
    app.setAppOrganisationGuid(orgIdentifier);

    String orgHandle = this.oAuthState.getOwnerOrgHandle();
    if (orgHandle == null) {
      orgHandle = this.authUser.getAudienceOwnerOrganizationHandle();
    }
    app.setAppOrganisationHandle(orgHandle);

    /**
     * OAuth Data
     */
    String oAuthHandle = this.oAuthState.getProviderHandle();
    if (oAuthHandle != null) {
      signInEvent.setOAuthProviderHandle(oAuthHandle);
    }
    String oAuthId = this.oAuthState.getProviderGuid();
    if (oAuthId != null) {
      signInEvent.setOAuthProviderGuid(oAuthId);
    }

    return signInEvent;
  }

  /**
   * Redirect without any cache
   */
  private void authenticationRedirect() {
    if (this.redirectUri == null) {
      try {
        /**
         * The default
         */
        this.redirectUri = this.getRedirectUriFromSessionOrClaims();
      } catch (TowerFailureException e) {
        // this exception is terminal
        // and the context was failed in the function
        // nothing to do
        return;
      }
    }
    ctx.response()
      // disable all caching
      .putHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
      .putHeader("Pragma", "no-cache")
      .putHeader(HttpHeaders.EXPIRES, "0")
      // redirect (when there is no state, redirect to home)
      .putHeader(HttpHeaders.LOCATION, this.redirectUri.toUrl().toString())
      .setStatusCode(TowerFailureTypeEnum.REDIRECT_SEE_OTHER_URI_303.getStatusCode())
      .end("Redirecting to " + this.redirectUri + ".");
  }

  /**
   * This function should be called once at the end
   * because the redirect uri may be changed by the authNContext handlers.
   * <p>
   * Example: This is the case with the redirection to the list registration confirmation
   * where the list user guid is known only after registration and the URL is then modified accordingly.
   * <p>
   * @return the redirect uri where to redirect the user after identification ore registration.
   * @throws TowerFailureException - This exception already fails the context if any error. We throw if any error, so that the code can stop its processing.
   */
  private UriEnhanced getRedirectUriFromSessionOrClaims() throws TowerFailureException {

    UriEnhanced redirection;
    if(this.oAuthState!=null){
      redirection = oAuthState.getRedirectUri();
      if(redirection!=null){
        return redirection;
      }
    }

    if(this.jwtClaims!=null){
      URI redirectionUri = jwtClaims.getRedirectUri();
      if(redirectionUri!=null){
        return UriEnhanced.createFromUri(redirectionUri);
      }
    }

    /**
     * Redirection to the client
     * (with code and state for third-party client)
     * We store the redirect uri in the session at registration
     * to be sure that this is the same device.
     * Otherwise, we store it in the claims, to handle case where the user for instance
     * switch of browser due to another default browser in the mail app
     */
    Session session = ctx.session();
    String sessionRedirectionUrl = session.remove(OAuthInternalSession.REDIRECT_URI_KEY);
    if (sessionRedirectionUrl == null) {
      throw TowerFailureException.builder()
        .setMessage("Your session was not found. The redirect uri was null. Are you using the same browser? ")
        .setType(TowerFailureTypeEnum.INTERNAL_ERROR_500)
        .setName("Session was not found")
        .setMimeToHtml()
        .buildWithContextFailing(ctx);
    }
    try {
      redirection = UriEnhanced.createFromString(sessionRedirectionUrl);
    } catch (IllegalStructure e) {
      String message = "An error prevents us to redirect you where you come from. The redirect uri (" + sessionRedirectionUrl + ") is not valid.";
      if (JavaEnvs.IS_DEV) {
        message += e.getMessage();
      }
      // internal error, we don't throw
      throw TowerFailureException.builder()
        .setMessage(message)
        .setType(TowerFailureTypeEnum.INTERNAL_ERROR_500)
        .setName("Bad URL redirect")
        .setMimeToHtml()
        .setCauseException(e)
        .buildWithContextFailing(ctx);
    }


    /**
     * If the client is a third-party client,
     * we had the code and the state
     * Note: the full client Oauth code type flow is not yet implemented
     * this code is therefore here for documentation purpose
     */
    String clientId = session.get(OAuthInternalSession.CLIENT_ID_KEY);
    if (clientId != null) {
      String inState = session.get(OAuthInternalSession.STATE_KEY);
      if (inState == null) {
        throw TowerFailureException.builder()
          .setMessage("The session state is null")
          .setType(TowerFailureTypeEnum.INTERNAL_ERROR_500)
          .setMimeToHtml()
          .buildWithContextFailing(ctx);
      }
      redirection.addQueryProperty(AuthQueryProperty.OAUTH_STATE.toString(), inState);
      String authCode = OAuthCodeManagement.createOrGet().createAuthorizationAndGetCode(sessionRedirectionUrl, authUser);
      redirection.addQueryProperty(AuthQueryProperty.CODE.toString(), authCode);
    }
    return redirection;

  }


}
