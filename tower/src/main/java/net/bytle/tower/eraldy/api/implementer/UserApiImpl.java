package net.bytle.tower.eraldy.api.implementer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.ValidationException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.UserApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithRealm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.model.openapi.UserPostBody;
import net.bytle.tower.eraldy.objectProvider.UserProvider;
import net.bytle.vertx.FailureStatic;
import net.bytle.vertx.HttpStatusEnum;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.VertxFailureHttp;

import java.util.List;
import java.util.stream.Collectors;

public class UserApiImpl implements UserApi {

  private final EraldyApiApp apiApp;
  private final ObjectMapper userMapper;

  public UserApiImpl(TowerApp towerApp) {
    this.apiApp = (EraldyApiApp) towerApp;
    this.userMapper = this.apiApp.getApexDomain().getHttpServer().getServer().getJacksonMapperManager().jsonMapperBuilder()
      .addMixIn(User.class, UserPublicMixinWithRealm.class)
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
            .getUserByEmail(userIdentifier, realm);
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

    User signedInUser;
    try {
      signedInUser = apiApp.getAuthSignedInUser(routingContext);
    } catch (NotFoundException e) {
      return Future.failedFuture(
        VertxFailureHttp.create()
          .setStatus(HttpStatusEnum.NOT_LOGGED_IN_401)
          .setDescription("The authenticated user was not found")
          .getFailedException()
      );
    }

    return apiApp.getUserProvider()
      .getUserByGuid(signedInUser.getGuid())
      .compose(user -> {
        ApiResponse<User> userApiResponse = new ApiResponse<>(user)
          .setMapper(this.userMapper);
        return Future.succeededFuture(userApiResponse);
      });


  }

  @Override
  public Future<ApiResponse<User>> userPost(RoutingContext routingContext, UserPostBody userPostBody) {


    User userRequested = new User();
    userRequested.setGuid(userPostBody.getUserGuid());
    userRequested.setEmail(userPostBody.getUserEmail());
    userRequested.setGivenName(userPostBody.getUserName());
    userRequested.setFullname(userPostBody.getUserFullname());
    userRequested.setTitle(userPostBody.getUserTitle());
    userRequested.setAvatar(userPostBody.getUserAvatar());

    UserProvider userProvider = apiApp.getUserProvider();
    return userProvider.getUserRealmAndUpdateUserIdEventuallyFromRequestData(userPostBody.getRealmIdentifier(), userRequested)
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(realm -> {
        userRequested.setRealm(realm);
        return userProvider.upsertUser(userRequested);
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
