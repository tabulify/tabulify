package net.bytle.tower.eraldy.api.implementer;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.RealmApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.tower.eraldy.objectProvider.UserProvider;
import net.bytle.vertx.*;

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
  public Future<ApiResponse<List<User>>> realmRealmUsersGet(RoutingContext routingContext, String realmIdentifier, Long pageSize, Long pageId, String searchTerm) {

    RoutingContextWrapper routingContextWrapper = RoutingContextWrapper.createFrom(routingContext);
    pageSize = routingContextWrapper.getRequestQueryParameterAsLong("pageSize",10L);
    pageId = routingContextWrapper.getRequestQueryParameterAsLong("pageId",0L);
    searchTerm = routingContextWrapper.getRequestQueryParameterAsString("searchTerm",null);

    UserProvider userProvider = apiApp.getUserProvider();
    Long finalPageId = pageId;
    Long finalPageSize = pageSize;
    String finalSearchTerm = searchTerm;
    return this.apiApp.getRealmProvider()
      .getRealmFromIdentifier(realmIdentifier)
      .compose(
        realm -> userProvider.getUsers(realm, finalPageId, finalPageSize, finalSearchTerm),
        err->Future.failedFuture(TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.INTERNAL_ERROR_500)
          .setMessage("Realm could not be retrieved with the identifier "+realmIdentifier)
          .setCauseException(err)
          .build()
        )
      )
      .compose(
        users -> Future.succeededFuture(new ApiResponse<>(users).setMapper(userProvider.getApiMapper())),
        err->Future.failedFuture(TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.INTERNAL_ERROR_500)
          .setMessage("Users could not be retrieved")
          .setCauseException(err)
          .build()
        )
      );

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
              .setType(TowerFailureTypeEnum.NOT_FOUND_404)
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
              .setCauseException(err)
              .setMessage("Unexpected problem while getting the realms owned by the user")
              .buildWithContextFailing(routingContext))
        ));

  }

}
