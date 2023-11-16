package net.bytle.tower.eraldy.auth;

import io.vertx.core.Future;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.vertx.HttpStatusEnum;
import net.bytle.vertx.RoutingContextWrapper;
import net.bytle.vertx.VertxFailureHttpException;

public class Authorization {
  public static Future<Boolean> checkForRealm(EraldyApiApp apiApp, RoutingContextWrapper routingContext, Realm requestedRealm) {

    User signedInUser;
    try {
      signedInUser = apiApp.getAuthSignedInUser(routingContext.getRoutingContext());
    } catch (NotFoundException e) {
      return notAuthorized(routingContext);
    }

    return apiApp
      .getRealmProvider()
      .getRealmsForOwner(signedInUser, Realm.class)
      .compose(userRealms -> {
        boolean authorized = false;
        for (Realm realm1 : userRealms) {
          if (realm1.getLocalId().equals(requestedRealm.getLocalId())) {
            authorized = true;
            break;
          }
        }
        if (!authorized) {
          return notAuthorized(routingContext);
        }
        return Future.succeededFuture(true);
      });

  }

  private static Future<Boolean> notAuthorized(RoutingContextWrapper routingContext) {

    return Future.failedFuture(
      VertxFailureHttpException.builder()
        .setStatus(HttpStatusEnum.NOT_AUTHORIZED_401)
        .buildWithContextFailing(routingContext.getRoutingContext())
    );
  }

}
