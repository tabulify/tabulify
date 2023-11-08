package net.bytle.tower.eraldy.api.implementer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.RealmApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.Authorization;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.tower.eraldy.mixin.OrganizationPublicMixin;
import net.bytle.tower.eraldy.mixin.RealmPublicMixin;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.tower.eraldy.objectProvider.UserProvider;
import net.bytle.vertx.*;

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
  /**
   * The json mapper for public realm
   * to create json without any localId and
   * double realm information
   */
  private final ObjectMapper publicRealmJsonMapper;

  public RealmApiImpl(@SuppressWarnings("unused") TowerApp towerApp) {
    this.apiApp = (EraldyApiApp) towerApp;
    this.publicRealmJsonMapper = this.apiApp.getApexDomain().getHttpServer().getServer().getJacksonMapperManager()
      .createNewJsonMapper()
      .addMixIn(Realm.class, RealmPublicMixin.class)
      .addMixIn(RealmAnalytics.class, RealmPublicMixin.class)
      .addMixIn(Organization.class, OrganizationPublicMixin.class)
      .addMixIn(User.class, UserPublicMixinWithoutRealm.class);
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
          return Future.failedFuture(
            VertxRoutingFailureData.create()
              .setStatus(HttpStatus.NOT_FOUND)
              .setDescription("The realm was not found")
              .getFailedException()
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
            VertxRoutingFailureData.create()
              .setStatus(HttpStatus.NOT_FOUND)
              .setDescription("The realm was not found")
              .getFailedException()
          );
        }
        ApiResponse<RealmAnalytics> result = new ApiResponse<>(realm)
          .setMapper(this.publicRealmJsonMapper);

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
      return Future.failedFuture(
        VertxRoutingFailureData.create()
          .setStatus(HttpStatus.NOT_FOUND)
          .setDescription("You should be logged in")
          .getFailedException()
      );
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
