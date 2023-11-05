package net.bytle.tower.eraldy.api.implementer;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.ValidationException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.UserApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.OrganizationUser;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.model.openapi.UserPostBody;
import net.bytle.tower.eraldy.objectProvider.UserProvider;
import net.bytle.tower.util.AuthInternalAuthenticator;
import net.bytle.vertx.FailureStatic;
import net.bytle.vertx.HttpStatus;
import net.bytle.vertx.TowerApp;

import java.util.List;
import java.util.stream.Collectors;

public class UserApiImpl implements UserApi {

  private final EraldyApiApp apiApp;

  public UserApiImpl(TowerApp towerApp) {
    this.apiApp = (EraldyApiApp) towerApp;
  }

  @Override
  public Future<ApiResponse<OrganizationUser>> userAuthGet(RoutingContext routingContext) {
    try {
      return AuthInternalAuthenticator.getAuthUserFromContext(apiApp, routingContext)
        .onFailure(routingContext::fail)
        .compose(comboUser -> Future.succeededFuture(new ApiResponse<>(comboUser)));
    } catch (NotFoundException e) {
      return Future.succeededFuture(new ApiResponse<>(HttpStatus.NOT_FOUND));
    }
  }

  @Override
  public Future<ApiResponse<User>> userGet(RoutingContext routingContext, String userGuid, String userEmail, String realmHandle, String realmGuid) {
    Vertx vertx = routingContext.vertx();
    Future<User> userFuture;
    UserProvider userProvider = apiApp.getUserProvider();
    if (userGuid != null) {
      userFuture = userProvider
        .getUserByGuid(userGuid);
    } else {
      if (userEmail == null) {
        throw ValidationException.create("A userGuid or an userEmail should be given", "userEmail", null);
      }
      if (realmHandle == null && realmGuid == null) {
        throw ValidationException.create("With a userEmail, a realm identifiant (realmGuid or realmHandle) should be given", "realmHandle", null);
      }
      userFuture = this.apiApp.getRealmProvider()
        .getRealmFromGuidOrHandle(realmGuid, realmHandle)
        .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
        .compose(realm -> {
          if (realm == null) {
            if (realmGuid != null) {
              throw ValidationException.create("The realmGuid does not exist", "realmGuid", realmGuid);
            }
            throw ValidationException.create("The realmHandle does not exist", "realmHandle", realmHandle);
          }
          return userProvider
            .getUserByEmail(userEmail, realm);
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

      try {
        return AuthInternalAuthenticator.getAuthUserFromContext(apiApp, routingContext)
          .onFailure(routingContext::fail)
          .compose(organizationUser -> {
            organizationUser.setOrganization(null);
            User publicUser = apiApp.getUserProvider()
              .toPublicCloneWithRealm(organizationUser);
            return Future.succeededFuture((new ApiResponse<>(publicUser)));
          });
      } catch (NotFoundException e) {
        return Future.succeededFuture((new ApiResponse<>(HttpStatus.NOT_AUTHORIZED)));
      }


  }

  @Override
  public Future<ApiResponse<User>> userPost(RoutingContext routingContext, UserPostBody userPostBody) {
    Vertx vertx = routingContext.vertx();


    Realm userRealmRequested = new Realm();
    userRealmRequested.setHandle(userPostBody.getRealmHandle());
    userRealmRequested.setGuid(userPostBody.getRealmGuid());

    User userRequested = new User();
    userRequested.setGuid(userPostBody.getUserGuid());
    userRequested.setEmail(userPostBody.getUserEmail());
    userRequested.setName(userPostBody.getUserName());
    userRequested.setFullname(userPostBody.getUserFullname());
    userRequested.setTitle(userPostBody.getUserTitle());
    userRequested.setAvatar(userPostBody.getUserAvatar());

    UserProvider userProvider = apiApp.getUserProvider();
    return userProvider.getUserRealmAndUpdateUserIdEventuallyFromRequestData(userRealmRequested, userRequested)
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
  public Future<ApiResponse<List<User>>> usersGet(RoutingContext routingContext, String realmGuid, String
    realmHandle) {

    UserProvider userProvider = apiApp.getUserProvider();
    return this.apiApp.getRealmProvider()
      .getRealmFromGuidOrHandle(realmGuid, realmHandle)
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
