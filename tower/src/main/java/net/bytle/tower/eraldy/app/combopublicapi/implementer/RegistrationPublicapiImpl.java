package net.bytle.tower.eraldy.app.combopublicapi.implementer;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.interfaces.RegistrationPublicapi;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.app.memberapp.implementer.flow.ListRegistrationFlow;
import net.bytle.tower.eraldy.model.openapi.ExitStatusResponse;
import net.bytle.tower.eraldy.model.openapi.ListRegistrationPostBody;

public class RegistrationPublicapiImpl implements RegistrationPublicapi {


  @Deprecated
  @Override
  public Future<ApiResponse<ExitStatusResponse>> listRegistrationPost(RoutingContext
                                                                        routingContext, ListRegistrationPostBody publicationSubscriptionPost) {

    return ListRegistrationFlow.handleStep1SendingValidationEmail(routingContext, publicationSubscriptionPost)
      .compose(response -> Future.succeededFuture(new ApiResponse<>()));


  }

}
