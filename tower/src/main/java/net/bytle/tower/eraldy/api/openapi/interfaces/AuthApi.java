package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.EmailIdentifier;
import net.bytle.tower.eraldy.model.openapi.OAuthAccessTokenResponse;
import net.bytle.tower.eraldy.model.openapi.PasswordCredentials;
import net.bytle.tower.eraldy.model.openapi.PasswordOnly;

public interface AuthApi  {

    /**
     * Login by sending an email with a login link
    */
    Future<ApiResponse<Void>> authLoginEmailPost(RoutingContext routingContext, EmailIdentifier emailIdentifier);

    /**
     * Exchange a code for an access token
    */
    Future<ApiResponse<OAuthAccessTokenResponse>> authLoginOauthAccessTokenGet(RoutingContext routingContext, String code, String clientId, String clientSecret, String redirectUri);

    /**
     * Redirect to the external oauth authorization end point
    */
    Future<ApiResponse<Void>> authLoginOauthProviderGet(RoutingContext routingContext, String provider, String listGuid, String redirectUri, String realmHandle, String realmGuid);

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
     * Register a user
    */
    Future<ApiResponse<Void>> authUserRegisterPost(RoutingContext routingContext, EmailIdentifier emailIdentifier);
}
