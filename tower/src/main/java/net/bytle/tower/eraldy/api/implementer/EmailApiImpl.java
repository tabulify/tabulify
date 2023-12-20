package net.bytle.tower.eraldy.api.implementer;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.EmailApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;

public class EmailApiImpl implements EmailApi {
  private final EraldyApiApp apiApp;

  public EmailApiImpl(TowerApp towerApp) {
    this.apiApp = (EraldyApiApp) towerApp;
  }

  @Override
  public Future<ApiResponse<JsonObject>> emailAddressAddressValidateGet(RoutingContext routingContext, String email) {

    return this.apiApp.getEmailAddressValidator()
      .validate(email, false)
      .compose(
        res -> {
          int statusCode = 200;
          if(!res.pass()){
            statusCode = TowerFailureTypeEnum.BAD_STRUCTURE_422.getStatusCode();
          }
          return Future.succeededFuture(new ApiResponse<>(statusCode,res.toJsonObject()));
        },
        err -> Future.failedFuture(TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.INTERNAL_ERROR_500)
          .setMessage("A runtime error has occurred during the email address validation.")
          .setCauseException(err)
          .build()
        )
      );

  }
}
