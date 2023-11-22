package net.bytle.tower.eraldy.api.implementer;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.RealmApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.AuthPermission;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.vertx.FailureStatic;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureStatusEnum;

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
  public Future<ApiResponse<List<User>>> realmUsersNewGet(RoutingContext routingContext, String realmIdentifier) {

    return this.apiApp.getRealmProvider()
      .getRealmFromIdentifierNotNull(realmIdentifier, Realm.class)
      .compose(realm -> this.apiApp.getAuthProvider().checkRealmAuthorization(realm, AuthPermission.REALM_ACTIVITY_NEW_USERS)
        .compose(realm1 -> apiApp.getUserProvider()
          .getRecentUsersCreatedFromRealm(realm)
          .compose(users -> Future.succeededFuture(new ApiResponse<>(users).setMapper(apiApp.getUserProvider().getApiMapper())))
        ));

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
    return apiApp.getOrganizationUserProvider()
      .getOrganizationUserByGuid(userGuid)
      .compose(user -> this.apiApp.getRealmProvider()
        .getRealmsForOwner(user, Realm.class)
        .compose(realms -> Future.succeededFuture(new ApiResponse<>(realms))));
  }

  @Override
  public Future<ApiResponse<List<RealmAnalytics>>> realmsOwnedByMeGet(RoutingContext routingContext) {

    return this.apiApp
      .getAuthProvider().getSignedInOrganizationalUser(routingContext)
      .compose(authSignedInUser -> this.apiApp
        .getRealmProvider()
        .getRealmsForOwner(authSignedInUser, RealmAnalytics.class)
        .compose(
          realms -> Future.succeededFuture(
            new ApiResponse<>(realms)
              .setMapper(apiApp.getRealmProvider().getPublicJsonMapper()))
          ,
          err -> Future.failedFuture(
            TowerFailureException
              .builder()
              .setException(err)
              .setMessage("Unexpected problem while getting the realms owned by the user")
              .buildWithContextFailing(routingContext))
        ));

  }

}
