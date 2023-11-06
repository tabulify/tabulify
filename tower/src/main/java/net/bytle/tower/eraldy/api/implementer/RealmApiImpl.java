package net.bytle.tower.eraldy.api.implementer;

import io.vertx.core.Future;
import io.vertx.ext.auth.authorization.RoleBasedAuthorization;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.RealmApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.Authorization;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.tower.eraldy.objectProvider.UserProvider;
import net.bytle.vertx.FailureStatic;
import net.bytle.vertx.HttpStatus;
import net.bytle.vertx.RoutingContextWrapper;
import net.bytle.vertx.TowerApp;

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
public class RealmApiImpl implements RealmApi {


  private final EraldyApiApp apiApp;

  public RealmApiImpl(@SuppressWarnings("unused") TowerApp towerApp) {
    this.apiApp = (EraldyApiApp) towerApp;
  }

  @Override
  public Future<ApiResponse<Realm>> realmPost(RoutingContext routingContext, RealmPostBody realmPost) {

    Realm realm = new Realm();
    String handle = realmPost.getHandle();
    realm.setHandle(handle);
    realm.setName(realmPost.getName());

    RealmProvider realmProvider = this.apiApp.getRealmProvider();
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

    return this.apiApp.getRealmProvider()
      .getRealmFromGuid(guid)
      .compose(realm -> {
        if (realm == null) {
          NotFoundException theRealmWasNotFound = new NotFoundException("The realm was not found");
          routingContext.fail(HttpStatus.NOT_FOUND, theRealmWasNotFound);
          return Future.failedFuture(theRealmWasNotFound);
        }
        UserProvider userProvider = apiApp.getUserProvider();
        return Authorization.checkForRealm(apiApp, RoutingContextWrapper.createFrom(routingContext), realm)
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

    RealmProvider realmProvider = this.apiApp.getRealmProvider();
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
  public Future<ApiResponse<List<RealmWithAppUris>>> realmsGet(RoutingContext routingContext) {

    if(!RoleBasedAuthorization.create("root").match(routingContext.user())){
      return Future.succeededFuture(new ApiResponse<>(HttpStatus.NOT_AUTHORIZED));
    }

    return  this.apiApp.getRealmProvider()
        .getRealmsWithAppUris()
        .compose(realms -> Future.succeededFuture(new ApiResponse<>(realms)));


  }

  @Override
  public Future<ApiResponse<List<Realm>>> realmsOwnedByGet(RoutingContext routingContext, String userGuid) {
    return apiApp.getUserProvider()
      .getUserByGuid(userGuid)
      .compose(user -> this.apiApp.getRealmProvider()
        .getRealmsForOwner(user, Realm.class)
        .compose(realms -> Future.succeededFuture(new ApiResponse<>(realms))));
  }

  @Override
  public Future<ApiResponse<List<RealmAnalytics>>> realmsOwnedByMeGet(RoutingContext routingContext) {

    io.vertx.ext.auth.User vertxUser;
    try {
      vertxUser = RoutingContextWrapper.createFrom(routingContext).getSignedInUser();
    } catch (NotFoundException e) {
      IllegalArgumentException youShouldBeLoggedIn = new IllegalArgumentException("You should be logged in");
      routingContext.fail(HttpStatus.NOT_AUTHORIZED, youShouldBeLoggedIn);
      return Future.failedFuture(youShouldBeLoggedIn);
    }
    User user = UsersUtil.vertxUserToEraldyUser(vertxUser);
    RealmProvider realmProvider = this.apiApp.getRealmProvider();
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

}
