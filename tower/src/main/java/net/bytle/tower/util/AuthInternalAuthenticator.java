package net.bytle.tower.util;

import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.json.schema.ValidationException;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.app.memberapp.EraldyMemberApp;
import net.bytle.tower.eraldy.auth.EraldySessionHandler;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.tower.eraldy.model.openapi.OrganizationUser;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.objectProvider.OrganizationUserProvider;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.HttpStatus;
import net.bytle.vertx.VertxRoutingFailureData;

/**
 * A central entry point to authenticate
 * in the whole member application
 * <p>
 * It will authenticate the session
 * and redirect the user
 */
public class AuthInternalAuthenticator {


  public static Config createWith(RoutingContext ctx, User user) {
    if (user.getLocalId() == null) {
      throw new InternalException("The authenticated user has no database id");
    }
    return new Config(ctx, user);
  }


  public static Future<OrganizationUser> getAuthUserFromContext(RoutingContext ctx) throws NotFoundException {
    io.vertx.ext.auth.User user = ctx.user();
    if (user == null) {
      throw new NotFoundException();
    }
    JsonObject principal = user.principal();

    String rootClaim = user.attributes().getString("rootClaim");
    if (rootClaim != null && rootClaim.equals("accessToken")) {
      // JWT
      String userGuid = user.principal().getString("sub");
      if (userGuid == null) {
        return Future.failedFuture(ValidationException.create("The sub is empty", "sub", null));
      }
      return OrganizationUserProvider.get(ctx.vertx())
        .getOrganizationUserByGuid(userGuid);
    }
    OrganizationUser userCombo = principal.mapTo(OrganizationUser.class);
    return Future.succeededFuture(userCombo);
  }

  public enum RedirectionMethod {
    HTTP, // via HTTP
    FRONTEND, // via a HTML confirmation page
    NONE, // the client do a post request and navigate via Javascript to the origin page
  }

  public static class Config {
    private final RoutingContext ctx;
    private final User user;
    private RedirectionMethod redirectVia = RedirectionMethod.HTTP;
    private String appOperationPath;
    /**
     * The redirect URI may not be mandatory
     * This is the case when the user subscribes to
     * a list and terminates the flow on the confirmation page
     */
    private boolean redirectUriIsMandatory = true;

    public Config(RoutingContext ctx, User user) {
      this.ctx = ctx;
      this.user = user;
    }

    /**
     * @param frontEndOperationPath - the React html front end app operation path to redirect
     * @return the object for chaining
     * We redirect to the frontend app to have a consistent design.
     * We don't validate the operation path for now other than with a visual / integration test.
     */
    public Config redirectViaFrontEnd(String frontEndOperationPath) {
      /**
       * We don't return an HTML template for consistency in the HTML app design
       * The user registers and gets the confirmation in the same design
       */
      this.redirectVia = RedirectionMethod.FRONTEND;
      this.appOperationPath = frontEndOperationPath;
      return this;
    }

    public Config redirectViaHttp() {
      this.redirectVia = RedirectionMethod.HTTP;
      return this;
    }

    public Config redirectViaClient() {
      this.redirectVia = RedirectionMethod.NONE;
      return this;
    }

    public void authenticate() {

      /**
       * Try to redirect
       * <p>
       * The URI should be before authentication because the session may
       * be regenerated when the user is not the same
       * <p>
       * It can happen if the user is from another realm,
       * Say I log in to the combo portal as user of the realm Eraldy,
       * then I want to subscribe to a list from another realm,
       * I click on Oauth and a new user is created
       */
      UriEnhanced redirectUri = null;
      try {
        redirectUri = this.getAndRemoveRedirectUri();
      } catch (IllegalStructure e) {

        String message = "An error prevents us to redirect you where you come from. The redirect uri was not valid.";
        if (Env.IS_DEV) {
          message += e.getMessage();
        }
        VertxRoutingFailureData.create()
          .setDescription(message)
          .setStatusCode(HttpStatus.INTERNAL_ERROR)
          .setName("Bad URL redirect")
          .failContextAsHtml(ctx);
        return;

      } catch (NotFoundException e) {
        if (this.redirectUriIsMandatory) {
          VertxRoutingFailureData.create()
            .setDescription("An error prevents us to redirect you where you come from. We can't find where you come from (the redirect uri).")
            .setStatusCode(HttpStatus.INTERNAL_ERROR)
            .setName("URL redirect was not found")
            .failContextAsHtml(ctx);
          return;
        }
      }

      OrganizationUser sessionUser = UsersUtil.vertxUserToEraldyOrganizationUser(ctx.user());

      Future<OrganizationUser> futureOrganizationUser;
      boolean sameUser;
      /**
       * If no user or not the same authenticated user
       */
      if (sessionUser != null && sessionUser.getGuid().equals(user.getGuid())) {

        sameUser = true;
        futureOrganizationUser = Future.succeededFuture(sessionUser);

      } else {

        sameUser = false;

        /**
         * If Eraldy user realm, add the organization if any
         */
        if (UsersUtil.isEraldyUser(user)) {

          futureOrganizationUser = OrganizationUserProvider.get(ctx.vertx())
            .getOrganizationUserById(user.getLocalId(), user);

        } else {

          OrganizationUser organizationUser = JsonObject.mapFrom(user).mapTo(OrganizationUser.class);
          futureOrganizationUser = Future.succeededFuture(organizationUser);

        }

      }

      UriEnhanced finalRedirectUri = redirectUri;
      futureOrganizationUser
        .onSuccess(authenticationUser -> {
          if (!sameUser) {
            JsonObject principal = JsonObject.mapFrom(authenticationUser);
            io.vertx.ext.auth.User contextUser = io.vertx.ext.auth.User.create(principal);
            ctx.setUser(contextUser);

            // the user has upgraded from unauthenticated to authenticated
            // session should be upgraded as recommended by owasp
            Session session = ctx.session();
            session.regenerateId();
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
              if (finalRedirectUri == null) {
                VertxRoutingFailureData.create()
                  .setStatusCode(HttpStatus.INTERNAL_ERROR)
                  .setName("URL redirect is mandatory for HTTP redirect")
                  .setDescription("For an http redirect, the redirect uri is mandatory and was not found")
                  .failContextAsHtml(ctx);
                return;
              }
              authenticationRedirect(finalRedirectUri);
              break;
            case FRONTEND:
              if (this.appOperationPath == null) {
                throw new InternalException("The app operation path for redirection should not be null");
              }
              UriEnhanced redirectToHtmlApp = EraldyMemberApp.get()
                .getPublicRequestUriForOperationPath(this.appOperationPath);
              if (finalRedirectUri != null) {
                redirectToHtmlApp.addQueryProperty(OAuthQueryProperty.REDIRECT_URI.toString(), finalRedirectUri.toUrl().toString());
              }
              authenticationRedirect(redirectToHtmlApp);
              break;
            case NONE:
              /**
               * The client does the redirect (Javascript)
               */
              break;
            default:
              throw new InternalException("The redirection method (" + this.redirectVia + ") is unknown");
          }

        });


    }

    /**
     * Redirect without any cache
     *
     * @param redirectionUrl - the redirection URI
     */
    private void authenticationRedirect(UriEnhanced redirectionUrl) {
      ctx.response()
        // disable all caching
        .putHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
        .putHeader("Pragma", "no-cache")
        .putHeader(HttpHeaders.EXPIRES, "0")
        // redirect (when there is no state, redirect to home)
        .putHeader(HttpHeaders.LOCATION, redirectionUrl.toUrl().toString())
        .setStatusCode(HttpStatus.REDIRECT_SEE_OTHER_URI)
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
      if (!redirection.getApexWithoutPort().equals(EraldyMemberApp.get().getApexDomain().getApexNameWithoutPort())) {
        String inState = session.get(OAuthInternalSession.STATE_KEY);
        if (inState == null) {
          throw new NotFoundException("The session state is null");
        }
        redirection.addQueryProperty(OAuthQueryProperty.STATE.toString(), inState);
        String authCode = OAuthCodeManagement.createOrGet().createAuthorizationAndGetCode(sessionRedirectionUrl, user);
        redirection.addQueryProperty(OAuthQueryProperty.CODE.toString(), authCode);
      }
      return redirection;
    }


    public Config setMandatoryRedirectUri(boolean b) {
      this.redirectUriIsMandatory = b;
      return this;
    }
  }
}
