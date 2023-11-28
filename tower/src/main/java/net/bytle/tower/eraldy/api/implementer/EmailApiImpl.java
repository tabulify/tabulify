package net.bytle.tower.eraldy.api.implementer;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.dns.DnsException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.EmailApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.util.EmailAddressNotValid;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;

public class EmailApiImpl implements EmailApi {
  private final EraldyApiApp apiApp;

  public EmailApiImpl(TowerApp towerApp) {
    this.apiApp = (EraldyApiApp) towerApp;
  }

  @Override
  public Future<ApiResponse<Void>> emailAddressAddressValidationGet(RoutingContext routingContext, String email) {
    try {
      this.apiApp.getEmailAddressValidator()
        .validate(email);
    } catch (DnsException e) {
      return Future.failedFuture(TowerFailureException.builder()
        .setType(TowerFailureTypeEnum.INTERNAL_ERROR_500)
        .setMessage("A DNS error has occurred during the validation")
        .setCauseException(e)
        .build()
      );
    } catch (EmailAddressNotValid e) {
      return Future.failedFuture(TowerFailureException.builder()
        .setType(TowerFailureTypeEnum.BAD_STRUCTURE_422)
        .setMessage("The email is not valid")
        .setCauseException(e)
        .build()
      );
    }
    return Future.succeededFuture(new ApiResponse<>());
  }

}
