package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.EmailIdentifier;
import net.bytle.tower.eraldy.model.openapi.OAuthAccessTokenResponse;
import net.bytle.tower.eraldy.model.openapi.PasswordCredentials;
import net.bytle.tower.eraldy.model.openapi.PasswordOnly;

public interface AuthApi  {
    Future<ApiResponse<Void>> authLoginEmailPost(RoutingContext routingContext, EmailIdentifier emailIdentifier);
    Future<ApiResponse<OAuthAccessTokenResponse>> authLoginOauthAccessTokenGet(RoutingContext routingContext, String code, String clientId, String clientSecret, String redirectUri);
    Future<ApiResponse<Void>> authLoginOauthProviderGet(RoutingContext routingContext, String provider, String listGuid, String redirectUri, String realmHandle, String realmGuid);
    Future<ApiResponse<Void>> authLoginPasswordPost(RoutingContext routingContext, PasswordCredentials passwordCredentials);
    Future<ApiResponse<Void>> authLoginPasswordResetPost(RoutingContext routingContext, EmailIdentifier emailIdentifier);
    Future<ApiResponse<Void>> authLoginPasswordUpdatePost(RoutingContext routingContext, PasswordOnly passwordOnly);
    Future<ApiResponse<Void>> authLogoutGet(RoutingContext routingContext, String redirectUri);
    Future<ApiResponse<Void>> authUserRegisterPost(RoutingContext routingContext, EmailIdentifier emailIdentifier);
}
