package net.bytle.tower.eraldy.auth;

import io.vertx.core.Future;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.vertx.HttpStatus;
import net.bytle.vertx.RoutingContextWrapper;
import net.bytle.vertx.VertxRoutingFailureData;

public class Authorization {
  public static Future<Boolean> checkForRealm(EraldyApiApp apiApp, RoutingContextWrapper routingContext, Realm requestedRealm) {

    io.vertx.ext.auth.User vertxUser;
    try {
      vertxUser = routingContext.getSignedInUser();
    } catch (NotFoundException e) {
      return notAuthorized(routingContext);
    }
    User signedInUser = UsersUtil.vertxUserToEraldyUser(vertxUser);
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
    VertxRoutingFailureData.create()
      .setStatus(HttpStatus.NOT_AUTHORIZED)
      .failContext(routingContext.getRoutingContext());
    return Future.succeededFuture(false);
  }

}
