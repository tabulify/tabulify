package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.ListUser;
import net.bytle.tower.eraldy.model.openapi.ListUserShort;

import java.util.List;

public interface RegistrationApi  {
  Future<ApiResponse<ListUser>> listRegistrationGet(RoutingContext routingContext, String guid, String listGuid, String subscriberEmail);
    Future<ApiResponse<String>> listRegistrationLetterConfirmationGet(RoutingContext routingContext, String subscriberName, String listGuid, String listName, String ownerName, String ownerEmail, String ownerLogo);
    Future<ApiResponse<String>> listRegistrationLetterValidationGet(RoutingContext routingContext, String listGuid, String subscriberName, String subscriberEmail, Boolean debug);
    Future<ApiResponse<String>> listRegistrationValidationGet(RoutingContext routingContext, String data);

  Future<ApiResponse<List<ListUserShort>>> listRegistrationsGet(RoutingContext routingContext, String listGuid);
}
