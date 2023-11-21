package net.bytle.tower.eraldy.api.implementer;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.RealmApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.Authorization;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.tower.eraldy.objectProvider.UserProvider;
import net.bytle.vertx.*;

import java.util.ArrayList;
import java.util.List;

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
          ApiResponse<Realm> response = new ApiResponse<>(newRealm)
            .setMapper(this.apiApp.getRealmProvider().getPublicJsonMapper());
          return Future.succeededFuture(response);
        }
      );

  }

  @Override
  public Future<ApiResponse<List<User>>> realmUsersNewGet(RoutingContext routingContext, String guid) {

    return this.apiApp.getRealmProvider()
      .getRealmFromGuid(guid)
      .compose(realm -> {
        if (realm == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setStatus(TowerFailureStatusEnum.NOT_FOUND_404)
              .setMessage("The realm was not found")
              .build()
          );
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
  public Future<ApiResponse<RealmAnalytics>> realmGet(RoutingContext routingContext, String realmIdentifier) {

    RealmProvider realmProvider = this.apiApp.getRealmProvider();
    return realmProvider
      .getRealmAnalyticsFromIdentifier(realmIdentifier)
      .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
      .compose(realm -> {
        if (realm == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setStatus(TowerFailureStatusEnum.NOT_FOUND_404)
              .setMessage("The realm was not found")
              .build()
          );
        }
        ApiResponse<RealmAnalytics> result = new ApiResponse<>(realm)
          .setMapper(this.apiApp.getRealmProvider().getPublicJsonMapper());

        return Future.succeededFuture(result);
      });
  }

  @Override
  public Future<ApiResponse<List<RealmWithAppUris>>> realmsGet(RoutingContext routingContext) {


    return this.apiApp.getRealmProvider()
      .getRealmsWithAppUris()
      .compose(realms -> Future.succeededFuture(new ApiResponse<>(realms)));


  }

  @Override
  public Future<ApiResponse<List<Realm>>> realmsOwnedByGet(RoutingContext routingContext, String userGuid) {
    return apiApp.getUserProvider()
      .getUserByGuid(userGuid, User.class)
      .compose(user -> this.apiApp.getRealmProvider()
        .getRealmsForOwner(user, Realm.class)
        .compose(realms -> Future.succeededFuture(new ApiResponse<>(realms))));
  }

  @Override
  public Future<ApiResponse<List<RealmAnalytics>>> realmsOwnedByMeGet(RoutingContext routingContext) {

    return this.apiApp.getAuthSignedInUser(routingContext, User.class)
      .compose(signedInUser -> {
        if (signedInUser == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setStatus(TowerFailureStatusEnum.NOT_LOGGED_IN_401)
              .setMessage("You should be logged in")
              .build()
          );
        }
        RealmProvider realmProvider = this.apiApp.getRealmProvider();
        return realmProvider
          .getRealmsForOwner(signedInUser, RealmAnalytics.class)
          .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
          .compose(realms -> Future.succeededFuture(
            new ApiResponse<>(realms)
              .setMapper(apiApp.getRealmProvider().getPublicJsonMapper()))
          );
      });


  }

}
