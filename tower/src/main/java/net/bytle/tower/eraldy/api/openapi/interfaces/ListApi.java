package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.*;

import java.util.List;

public interface ListApi  {
    Future<ApiResponse<RegistrationList>> listGet(RoutingContext routingContext, String listGuid, String listHandle, String realmHandle);
    Future<ApiResponse<RegistrationList>> listPost(RoutingContext routingContext, ListPostBody listPostBody);
    Future<ApiResponse<String>> listRegisterConfirmationRegistrationGet(RoutingContext routingContext, String registrationGuid, String redirectUri);
    Future<ApiResponse<Void>> listRegisterPost(RoutingContext routingContext, ListRegistrationPostBody listRegistrationPostBody);
    Future<ApiResponse<Registration>> listRegistrationGet(RoutingContext routingContext, String guid, String listGuid, String subscriberEmail);
    Future<ApiResponse<String>> listRegistrationLetterConfirmationGet(RoutingContext routingContext, String subscriberName, String listGuid, String listName, String ownerName, String ownerEmail, String ownerLogo);
    Future<ApiResponse<String>> listRegistrationLetterValidationGet(RoutingContext routingContext, String listGuid, String subscriberName, String subscriberEmail, Boolean debug);
    Future<ApiResponse<String>> listRegistrationValidationGet(RoutingContext routingContext, String data);
    Future<ApiResponse<List<RegistrationShort>>> listRegistrationsGet(RoutingContext routingContext, String listGuid);
    Future<ApiResponse<List<RegistrationList>>> listsGet(RoutingContext routingContext, String appGuid, String appUri, String realmGuid, String realmHandle);
    Future<ApiResponse<List<ListSummary>>> listsSummaryGet(RoutingContext routingContext, String realmGuid, String realmHandle);
}
