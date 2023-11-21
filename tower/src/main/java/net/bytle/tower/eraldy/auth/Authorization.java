package net.bytle.tower.eraldy.auth;

import io.vertx.core.Future;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.OrganizationUser;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.vertx.RoutingContextWrapper;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureStatusEnum;
import net.bytle.vertx.auth.AuthUser;

public class Authorization {

  public static Future<Boolean> checkForRealm(EraldyApiApp apiApp, RoutingContextWrapper routingContext, Realm requestedRealm) {

    AuthUser signedInUser;
    try {
      signedInUser = apiApp.getAuthSignedInUser(routingContext.getRoutingContext());
    } catch (NotFoundException e) {
      return Future.failedFuture(
        TowerFailureException.builder()
          .setStatus(TowerFailureStatusEnum.NOT_LOGGED_IN_401)
          .buildWithContextFailing(routingContext.getRoutingContext())
      );
    }

    OrganizationUser organizationUser = UsersUtil.toOrganizationUser(signedInUser, apiApp);
    return apiApp
      .getRealmProvider()
      .getRealmsForOwner(organizationUser, Realm.class)
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
              .setMessage("Authenticated User has no permission on the requested realm (" + requestedRealm + ")")
              .buildWithContextFailing(routingContext.getRoutingContext())
          );
        }
        return Future.succeededFuture(true);
      });

  }

}
