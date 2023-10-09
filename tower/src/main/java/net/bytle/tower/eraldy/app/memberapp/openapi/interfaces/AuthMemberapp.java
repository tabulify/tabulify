package net.bytle.tower.eraldy.app.memberapp.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.app.memberapp.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.*;

public interface AuthMemberapp  {
    Future<ApiResponse<String>> loginEmailGet(RoutingContext routingContext, String redirectUri, String clientId, String responseType, String state, String realmHandle);
    Future<ApiResponse<Void>> loginEmailPost(RoutingContext routingContext, EmailIdentifier emailIdentifier);
    Future<ApiResponse<String>> loginGet(RoutingContext routingContext, String redirectUri, String clientId, String responseType, String state, String realmHandle);
    Future<ApiResponse<OAuthAccessTokenResponse>> loginOauthAccessTokenGet(RoutingContext routingContext, String code, String clientId, String clientSecret, String redirectUri);
    Future<ApiResponse<String>> loginOauthAuthorizeGet(RoutingContext routingContext, String redirectUri, String clientId, String responseType, String state);
    Future<ApiResponse<Void>> loginOauthProviderGet(RoutingContext routingContext, String provider, String listGuid, String redirectUri, String realmHandle, String realmGuid);
    Future<ApiResponse<String>> loginPasswordGet(RoutingContext routingContext, String redirectUri, String clientId, String responseType, String state, String realmHandle);
    Future<ApiResponse<Void>> loginPasswordPost(RoutingContext routingContext, PasswordCredentials passwordCredentials);
    Future<ApiResponse<String>> loginPasswordResetGet(RoutingContext routingContext, String redirectUri, String clientId, String responseType, String state, String realmHandle);
    Future<ApiResponse<Void>> loginPasswordResetPost(RoutingContext routingContext, EmailIdentifier emailIdentifier);
    Future<ApiResponse<String>> loginPasswordUpdateGet(RoutingContext routingContext);
    Future<ApiResponse<Void>> loginPasswordUpdatePost(RoutingContext routingContext, PasswordOnly passwordOnly);
    Future<ApiResponse<Void>> logoutGet(RoutingContext routingContext, String redirectUri);
    Future<ApiResponse<String>> registerListConfirmationRegistrationGet(RoutingContext routingContext, String registrationGuid, String redirectUri);
    Future<ApiResponse<String>> registerListListGet(RoutingContext routingContext, String listGuid, String redirectUri);
    Future<ApiResponse<Void>> registerListPost(RoutingContext routingContext, ListRegistrationPostBody listRegistrationPostBody);
    Future<ApiResponse<String>> registerUserGet(RoutingContext routingContext, String redirectUri, String clientId, String responseType, String state, String realmHandle);
    Future<ApiResponse<Void>> registerUserPost(RoutingContext routingContext, EmailIdentifier emailIdentifier);
}
