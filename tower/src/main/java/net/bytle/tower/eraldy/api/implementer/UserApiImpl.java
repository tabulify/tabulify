package net.bytle.tower.eraldy.api.implementer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.ValidationException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.UserApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.mixin.AppPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.mixin.RealmPublicMixin;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithRealm;
import net.bytle.tower.eraldy.model.openapi.App;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.model.openapi.UserPostBody;
import net.bytle.tower.eraldy.objectProvider.UserProvider;
import net.bytle.vertx.FailureStatic;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureStatusEnum;

import java.util.List;
import java.util.stream.Collectors;

public class UserApiImpl implements UserApi {

  private final EraldyApiApp apiApp;
  private final ObjectMapper userMapper;

  public UserApiImpl(TowerApp towerApp) {
    this.apiApp = (EraldyApiApp) towerApp;
    this.userMapper = this.apiApp.getApexDomain().getHttpServer().getServer().getJacksonMapperManager().jsonMapperBuilder()
      .addMixIn(User.class, UserPublicMixinWithRealm.class)
      .addMixIn(Realm.class, RealmPublicMixin.class)
      .addMixIn(App.class, AppPublicMixinWithoutRealm.class)
      .build();
  }


  @Override
  public Future<ApiResponse<User>> userGet(RoutingContext routingContext, String userIdentifier, String realmIdentifier) {

    Future<User> userFuture;
    UserProvider userProvider = apiApp.getUserProvider();

    if (userIdentifier.startsWith(UserProvider.USR_GUID_PREFIX)) {
      userFuture = userProvider
        .getUserByGuid(userIdentifier);
    } else {
      if (realmIdentifier == null) {
        throw ValidationException.create("With a userEmail, a realm identifier (guid or handle) should be given", "realmIdentifier", null);
      }
      userFuture = this.apiApp.getRealmProvider()
        .getRealmFromIdentifier(realmIdentifier)
        .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
        .compose(realm -> {
          if (realm == null) {
            throw ValidationException.create("The realm does not exist", "realmIdentifier", realmIdentifier);
          }
          return userProvider
            .getUserByEmail(userIdentifier, realm.getLocalId(), realm);
        });
    }
    return userFuture
      .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
      .compose(user -> {
        userProvider.toPublicCloneWithoutRealm(user);
        ApiResponse<User> apiResponse = new ApiResponse<>(user);
        return Future.succeededFuture(apiResponse);
      });

  }

  @Override
  public Future<ApiResponse<User>> userGuidGet(RoutingContext routingContext, String guid) {
    return apiApp.getUserProvider()
      .getUserByGuid(guid)
      .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
      .compose(user -> {
        ApiResponse<User> apiResponse = new ApiResponse<>(user);
        return Future.succeededFuture(apiResponse);
      });
  }

  @Override
  public Future<ApiResponse<User>> userMeGet(RoutingContext routingContext) {

    return apiApp.getAuthSignedInUser(routingContext, User.class)
      .compose(signedInUser -> {

        if (signedInUser == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setStatus(TowerFailureStatusEnum.NOT_LOGGED_IN_401)
              .setMessage("The authenticated user was not found")
              .build()
          );
        }
        return apiApp.getUserProvider()
          .getUserByGuid(signedInUser.getGuid())
          .compose(user -> {
            ApiResponse<User> userApiResponse = new ApiResponse<>(user)
              .setMapper(this.userMapper);
            return Future.succeededFuture(userApiResponse);
          });
      });


  }

  @Override
  public Future<ApiResponse<User>> userPost(RoutingContext routingContext, UserPostBody userPostBody) {


    User userRequested = new User();
    userRequested.setGuid(userPostBody.getUserGuid());
    userRequested.setEmail(userPostBody.getUserEmail());
    userRequested.setGivenName(userPostBody.getUserName());
    userRequested.setFullName(userPostBody.getUserFullname());
    userRequested.setTitle(userPostBody.getUserTitle());
    userRequested.setAvatar(userPostBody.getUserAvatar());

    UserProvider userProvider = apiApp.getUserProvider();
    return userProvider.getUserRealmAndUpdateUserIdEventuallyFromRequestData(userPostBody.getRealmIdentifier(), userRequested)
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(realm -> {
        userRequested.setRealm(realm);
        return userProvider.upsertUser(userRequested, routingContext);
      })
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(userUpserted -> {
        userProvider.toPublicCloneWithoutRealm(userUpserted);
        return Future.succeededFuture(new ApiResponse<>(userUpserted));
      });

  }


  @Override
  public Future<ApiResponse<List<User>>> usersGet(RoutingContext routingContext, String realmIdentifier) {

    UserProvider userProvider = apiApp.getUserProvider();
    return this.apiApp.getRealmProvider()
      .getRealmFromIdentifier(realmIdentifier)
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(userProvider::getUsers)
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(users -> {
        java.util.List<User> publicUsers = users
          .stream()
          .map(userProvider::toPublicCloneWithoutRealm)
          .collect(Collectors.toList());
        return Future.succeededFuture(new ApiResponse<>(publicUsers));
      });
  }

}
