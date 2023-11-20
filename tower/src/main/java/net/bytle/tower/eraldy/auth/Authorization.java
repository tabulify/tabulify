package net.bytle.tower.eraldy.auth;

import io.vertx.core.Future;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.vertx.RoutingContextWrapper;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureStatusEnum;

public class Authorization {

  public static Future<Boolean> checkForRealm(EraldyApiApp apiApp, RoutingContextWrapper routingContext, Realm requestedRealm) {

    return apiApp.getAuthSignedInUser(routingContext.getRoutingContext(), User.class)
      .compose(signedInUser->{
        if(signedInUser==null){
          return Future.failedFuture(
            TowerFailureException.builder()
              .setStatus(TowerFailureStatusEnum.NOT_LOGGED_IN_401)
              .buildWithContextFailing(routingContext.getRoutingContext())
          );
        }
        return apiApp
          .getRealmProvider()
          .getRealmsForOwner(signedInUser, Realm.class)
          .compose(userRealms -> {
            boolean authorized = false;
            for (Realm userRealm : userRealms) {
              if (userRealm.getLocalId().equals(requestedRealm.getLocalId())) {
                authorized = true;
                break;
              }
            }
            if (!authorized) {
              return Future.failedFuture(
                TowerFailureException.builder()
                  .setStatus(TowerFailureStatusEnum.NOT_AUTHORIZED_403)
                  .setMessage( "Authenticated User has no permission on the requested realm ("+requestedRealm+")")
                  .buildWithContextFailing(routingContext.getRoutingContext())
              );
            }
            return Future.succeededFuture(true);
          });
      });

  }



}
