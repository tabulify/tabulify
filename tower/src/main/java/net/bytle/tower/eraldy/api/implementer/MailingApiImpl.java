package net.bytle.tower.eraldy.api.implementer;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.MailingApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.AuthUserScope;
import net.bytle.tower.eraldy.model.openapi.Mailing;
import net.bytle.tower.eraldy.objectProvider.MailingProvider;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;

public class MailingApiImpl implements MailingApi {


  private final EraldyApiApp apiApp;

  public MailingApiImpl(TowerApp towerApp) {
    this.apiApp = (EraldyApiApp) towerApp;
  }

  @Override
  public Future<ApiResponse<Mailing>> mailingIdentifierGet(RoutingContext routingContext, String mailingGuidIdentifier) {

    MailingProvider mailingProvider = this.apiApp.getMailingProvider();
    Guid guid;
    try {
      guid = mailingProvider.getGuid(mailingGuidIdentifier);
    } catch (CastException e) {
      return Future.failedFuture(new IllegalArgumentException("The mailing guid (" + mailingGuidIdentifier + ") is not valid", e));
    }

    return this.apiApp.getRealmProvider()
      .getRealmFromLocalId(guid.getRealmOrOrganizationId())
      .compose(realm -> {
        if (realm == null) {
          return Future.failedFuture(TowerFailureException.builder()
            .setType(TowerFailureTypeEnum.NOT_FOUND_404) // our fault?, deleted a realm is pretty rare.
            .setMessage("The realm of the mailing (" + mailingGuidIdentifier + ") was not found")
            .build()
          );
        }
        return this.apiApp.getAuthProvider().checkRealmAuthorization(routingContext, realm, AuthUserScope.MAILING_GET);
      })
      .compose(realm -> {
        long localId = guid.validateRealmAndGetFirstObjectId(realm.getLocalId());
        return mailingProvider.getByLocalId(localId, realm);
      })
      .compose(mailing -> {
        if (mailing == null) {
          return Future.failedFuture(TowerFailureException.builder()
            .setType(TowerFailureTypeEnum.NOT_FOUND_404) // our fault
            .setMessage("The mailing (" + mailingGuidIdentifier + ") was not found")
            .build()
          );
        }
        return Future.succeededFuture(new ApiResponse<>(mailing).setMapper(mailingProvider.getApiMapper()));
      });

  }

}
