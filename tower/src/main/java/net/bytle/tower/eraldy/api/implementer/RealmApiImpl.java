package net.bytle.tower.eraldy.api.implementer;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.RealmApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.AuthUserScope;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.module.list.db.ListProvider;
import net.bytle.tower.eraldy.objectProvider.AuthProvider;
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
  public Future<ApiResponse<net.bytle.tower.eraldy.model.openapi.Realm>> realmPost(RoutingContext routingContext, RealmPostBody realmPost) {

    net.bytle.tower.eraldy.model.openapi.Realm realm = new net.bytle.tower.eraldy.model.openapi.Realm();
    String handle = realmPost.getHandle();
    realm.setHandle(handle);
    realm.setName(realmPost.getName());

    RealmProvider realmProvider = this.apiApp.getRealmProvider();
    return realmProvider
      .upsertRealm(realm)
      .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
      .compose(newRealm -> {
          ApiResponse<net.bytle.tower.eraldy.model.openapi.Realm> response = new ApiResponse<>(newRealm)
            .setMapper(this.apiApp.getRealmProvider().getPublicJsonMapper());
          return Future.succeededFuture(response);
        }
      );

  }

  @Override
  public Future<ApiResponse<List<ListObject>>> realmRealmIdentifierListsGet(RoutingContext routingContext, String realmIdentifier) {
    ListProvider listProvider = this.apiApp.getListProvider();
    AuthProvider authProvider = this.apiApp.getAuthProvider();
    return this.apiApp.getRealmProvider()
      .getRealmFromIdentifier(realmIdentifier)
      .compose(realm-> authProvider.checkRealmAuthorization(routingContext, realm, AuthUserScope.REALM_LISTS_GET))
      .compose(listProvider::getListsForRealm)
      .compose(lists->Future.succeededFuture(new ApiResponse<>(lists).setMapper(listProvider.getApiMapper())));
  }

  @Override
  public Future<ApiResponse<List<User>>> realmRealmUsersGet(RoutingContext routingContext, String realmIdentifier, Long pageSize, Long pageId, String searchTerm) {

    RoutingContextWrapper routingContextWrapper = RoutingContextWrapper.createFrom(routingContext);
    pageSize = routingContextWrapper.getRequestQueryParameterAsLong("pageSize", 10L);
    pageId = routingContextWrapper.getRequestQueryParameterAsLong("pageId", 1L);
    searchTerm = routingContextWrapper.getRequestQueryParameterAsString("searchTerm", "");

    UserProvider userProvider = apiApp.getUserProvider();
    Long finalPageId = pageId;
    Long finalPageSize = pageSize;
    String finalSearchTerm = searchTerm;
    return this.apiApp.getRealmProvider()
      .getRealmFromIdentifier(realmIdentifier)
      .compose(
        realm -> userProvider.getUsers(realm, finalPageId, finalPageSize, finalSearchTerm),
        err -> Future.failedFuture(TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.INTERNAL_ERROR_500)
          .setMessage("Realm could not be retrieved with the identifier " + realmIdentifier)
          .setCauseException(err)
          .build()
        )
      )
      .compose(
        users -> Future.succeededFuture(new ApiResponse<>(users).setMapper(userProvider.getApiMapper())),
        err -> Future.failedFuture(TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.INTERNAL_ERROR_500)
          .setMessage("Users could not be retrieved")
          .setCauseException(err)
          .build()
        )
      );

  }


  @Override
  public Future<ApiResponse<Realm>> realmGet(RoutingContext routingContext, String realmIdentifier) {

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
        ApiResponse<Realm> result = new ApiResponse<>(realm)
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
  public Future<ApiResponse<List<net.bytle.tower.eraldy.model.openapi.Realm>>> realmsOwnedByGet(RoutingContext routingContext, String userGuid) {
    return apiApp.getOrganizationUserProvider()
      .getOrganizationUserByIdentifier(userGuid)
      .compose(user -> this.apiApp.getRealmProvider()
        .getRealmsForOwner(user)
        .compose(realms -> Future.succeededFuture(new ApiResponse<>(new ArrayList<>(realms)))));
  }

  @Override
  public Future<ApiResponse<List<Realm>>> realmsOwnedByMeGet(RoutingContext routingContext) {

    return this.apiApp
      .getAuthProvider().getSignedInOrganizationalUser(routingContext)
      .compose(authSignedInUser -> this.apiApp
        .getRealmProvider()
        .getRealmsForOwner(authSignedInUser)
        .compose(
          realms -> {
            List<Realm> realmAnalytics = new ArrayList<>(realms);
            return Future.succeededFuture(
              new ApiResponse<>(realmAnalytics)
                .setMapper(apiApp.getRealmProvider().getPublicJsonMapper())
            );
          },
          err -> Future.failedFuture(
            TowerFailureException
              .builder()
              .setCauseException(err)
              .setMessage("Unexpected problem while getting the realms owned by the user")
              .buildWithContextFailing(routingContext))
        ));

  }

}
