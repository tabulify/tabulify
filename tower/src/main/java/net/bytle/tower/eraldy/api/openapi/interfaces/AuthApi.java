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
     * Login or register a user by sending an email with a link  This is one login/registration endpoint.  This is better than a login and a register endpoint because the user will always get an email. ie to avoid leaking if the user is registered, (ie Email enumeration protection), the login endpoint does not return any information (send no email).
    */
    Future<ApiResponse<Void>> authLoginEmailPost(RoutingContext routingContext, AuthEmailPost authEmailPost);

    /**
     * Exchange a oauth code for an access token
    */
    Future<ApiResponse<OAuthAccessTokenResponse>> authLoginOauthAccessTokenGet(RoutingContext routingContext, String code, String clientId, String clientSecret, String redirectUri);

    /**
     * Redirect to the external oauth authorization end point
    */
    Future<ApiResponse<Void>> authLoginOauthProviderGet(RoutingContext routingContext, String provider, String redirectUri, String clientId, String listGuid);

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
    Future<ApiResponse<Void>> authLogoutGet(RoutingContext routingContext, String clientId, String redirectUri);
}
