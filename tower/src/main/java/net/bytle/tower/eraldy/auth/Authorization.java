package net.bytle.tower.eraldy.auth;

import io.vertx.core.Future;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.tower.util.HttpStatus;
import net.bytle.tower.util.RoutingContextWrapper;

public class Authorization {
  public static Future<Boolean> checkForRealm(RoutingContextWrapper routingContext, Realm requestedRealm) {

    User signedInUser;
    try {
      signedInUser = routingContext.getSignedInUser();
    } catch (NotFoundException e) {
      return notAuthorized(routingContext);
    }

    return RealmProvider.createFrom(routingContext.getVertx())
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
    routingContext.getRoutingContext().fail(HttpStatus.NOT_AUTHORIZED);
    return Future.failedFuture("Not authorized");
  }
}
