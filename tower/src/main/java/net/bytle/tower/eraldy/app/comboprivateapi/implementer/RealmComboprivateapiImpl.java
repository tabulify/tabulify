package net.bytle.tower.eraldy.app.comboprivateapi.implementer;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.app.comboprivateapi.openapi.interfaces.RealmComboprivateapi;
import net.bytle.tower.eraldy.app.comboprivateapi.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.Authorization;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.RealmAnalytics;
import net.bytle.tower.eraldy.model.openapi.RealmPostBody;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.tower.eraldy.objectProvider.UserProvider;
import net.bytle.tower.util.FailureStatic;
import net.bytle.tower.util.HttpStatus;
import net.bytle.tower.util.RoutingContextWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A protection/user space
 * There is only one user by username by realm
 * <p>
 * Note that the scope of the credentials are
 * <a href="https://httpwg.org/specs/rfc7617.html#reusing.credentials">URL based</a>
 */
public class RealmComboprivateapiImpl implements RealmComboprivateapi {


  @Override
  public Future<ApiResponse<Realm>> realmPost(RoutingContext routingContext, RealmPostBody realmPost) {

    Realm realm = new Realm();
    String handle = realmPost.getHandle();
    realm.setHandle(handle);
    realm.setName(realmPost.getName());

    Vertx vertx = routingContext.vertx();
    RealmProvider realmProvider = RealmProvider.createFrom(vertx);
    return realmProvider
      .upsertRealm(realm)
      .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
      .compose(newRealm -> {
          Realm newRealmPublic = realmProvider.toPublicClone(newRealm);
          return Future.succeededFuture(new ApiResponse<>(newRealmPublic));
        }
      );

  }

  @Override
  public Future<ApiResponse<List<User>>> realmUsersNewGet(RoutingContext routingContext, String guid) {

    return RealmProvider.createFrom(routingContext.vertx())
      .getRealmFromGuid(guid)
      .compose(realm -> {
        if (realm == null) {
          NotFoundException theRealmWasNotFound = new NotFoundException("The realm was not found");
          routingContext.fail(HttpStatus.NOT_FOUND, theRealmWasNotFound);
          return Future.failedFuture(theRealmWasNotFound);
        }
        UserProvider userProvider = UserProvider.createFrom(routingContext.vertx());
        return Authorization.checkForRealm(RoutingContextWrapper.createFrom(routingContext), realm)
          .compose(bool -> userProvider
            .getRecentUsersCreatedFromRealm(realm)
            .compose(users -> {
              List<User> publicUsers = new ArrayList<>();
              for (User user : users) {
                publicUsers.add(userProvider.toPublicCloneWithoutRealm(user));
              }
              return Future.succeededFuture(new ApiResponse<>(publicUsers));
            })
          );
      });

  }


  @Override
  public Future<ApiResponse<RealmAnalytics>> realmGet(RoutingContext routingContext, String realmGuid, String realmHandle) {
    Vertx vertx = routingContext.vertx();
    RealmProvider realmProvider = RealmProvider.createFrom(vertx);
    return realmProvider
      .getRealmAnalyticsFromGuidOrHandle(realmGuid, realmHandle)
      .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
      .compose(realm -> {
        if (realm == null) {
          return Future.succeededFuture(new ApiResponse<>(HttpStatus.BAD_REQUEST));
        }
        RealmAnalytics publicRealm = realmProvider.toPublicClone(realm);
        return Future.succeededFuture(new ApiResponse<>(publicRealm));
      });
  }

  @Override
  public Future<ApiResponse<List<RealmAnalytics>>> realmsGet(RoutingContext routingContext) {


    Vertx vertx = routingContext.vertx();
    User user;
    try {
      user = RoutingContextWrapper.createFrom(routingContext).getSignedInUser();
    } catch (NotFoundException e) {
      IllegalArgumentException youShouldBeLoggedIn = new IllegalArgumentException("You should be logged in");
      routingContext.fail(HttpStatus.NOT_AUTHORIZED, youShouldBeLoggedIn);
      return Future.failedFuture(youShouldBeLoggedIn);
    }
    RealmProvider realmProvider = RealmProvider.createFrom(vertx);
    return realmProvider
      .getRealmsForOwner(user, RealmAnalytics.class)
      .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
      .compose(realms -> {
        List<RealmAnalytics> realmPublics = realms.stream()
          .map(realmProvider::toPublicClone)
          .collect(Collectors.toList());
        return Future.succeededFuture(new ApiResponse<>(realmPublics));
      });

  }

  @Override
  public Future<ApiResponse<List<Realm>>> realmsOwnedByGet(RoutingContext routingContext, String userGuid) {
    return UserProvider.createFrom(routingContext.vertx())
      .getUserByGuid(userGuid)
      .compose(user -> RealmProvider.createFrom(routingContext.vertx())
        .getRealmsForOwner(user, Realm.class)
        .compose(realms -> Future.succeededFuture(new ApiResponse<>(realms))));
  }

}
