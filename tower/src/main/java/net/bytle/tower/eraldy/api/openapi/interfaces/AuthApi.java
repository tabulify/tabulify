package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.*;

public interface AuthApi  {

    /**
     * Authorize is the main entrypoint for the login flow.  If the user is already logged, the user is redirected to the redirect uri. If the user is not logged in, the user is redirected to the front end app.  It's an adaptation of the [OAuth authorization endpoint](https://datacadamia.com/iam/oauth/authorization_endpoint)
    */
    Future<ApiResponse<Void>> authLoginAuthorizeGet(RoutingContext routingContext, String redirectUri);

    /**
     * Login by sending an email with a login link
    */
    Future<ApiResponse<Void>> authLoginEmailPost(RoutingContext routingContext, EmailIdentifier emailIdentifier);

    /**
     * Exchange a oauth code for an access token
    */
    Future<ApiResponse<OAuthAccessTokenResponse>> authLoginOauthAccessTokenGet(RoutingContext routingContext, String code, String clientId, String clientSecret, String redirectUri);

    /**
     * Redirect to the external oauth authorization end point
    */
    Future<ApiResponse<Void>> authLoginOauthProviderGet(RoutingContext routingContext, String provider, String redirectUri, String realmIdentifier, String listGuid);

    /**
     * The login form end point for password credentials
    */
    Future<ApiResponse<Void>> authLoginPasswordPost(RoutingContext routingContext, PasswordCredentials passwordCredentials);

    /**
     * Login via a link and give the possibility to modify the password
    */
    Future<ApiResponse<Void>> authLoginPasswordResetPost(RoutingContext routingContext, EmailIdentifier emailIdentifier);

    /**
     * Update the password
    */
    Future<ApiResponse<Void>> authLoginPasswordUpdatePost(RoutingContext routingContext, PasswordOnly passwordOnly);

    /**
     * The logout endpoint
    */
    Future<ApiResponse<Void>> authLogoutGet(RoutingContext routingContext, String redirectUri);

    /**
     * Register a user to a list If the user is:   * not signed in, a redirection occurs to the page with the register form   * signed in, a redirection occurs to the confirmation page
    */
    Future<ApiResponse<Void>> authRegisterListListGuidGet(RoutingContext routingContext, String listGuid);

    /**
     * Register a user to a list by sending an email for validation
    */
    Future<ApiResponse<Void>> authRegisterListPost(RoutingContext routingContext, ListRegistrationPostBody listRegistrationPostBody);

    /**
     * Register a user
    */
    Future<ApiResponse<Void>> authRegisterUserPost(RoutingContext routingContext, UserRegisterPost userRegisterPost);
}
