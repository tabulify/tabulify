package net.bytle.tower.eraldy.app.comboprivateapi.implementer;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.ValidationException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.app.comboprivateapi.openapi.interfaces.UserComboprivateapi;
import net.bytle.tower.eraldy.app.comboprivateapi.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.OrganizationUser;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.model.openapi.UserPostBody;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.tower.eraldy.objectProvider.UserProvider;
import net.bytle.tower.util.AuthInternalAuthenticator;
import net.bytle.tower.util.FailureStatic;
import net.bytle.tower.util.HttpStatus;

import java.util.List;
import java.util.stream.Collectors;

public class UserComboprivateapiImpl implements UserComboprivateapi {

  @Override
  public Future<ApiResponse<OrganizationUser>> userAuthGet(RoutingContext routingContext) {
    try {
      return AuthInternalAuthenticator.getAuthUserFromContext(routingContext)
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
    UserProvider userProvider = UserProvider.createFrom(vertx);
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
      userFuture = RealmProvider.createFrom(vertx)
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
    Vertx vertx = routingContext.vertx();
    return UserProvider.createFrom(vertx)
      .getUserByGuid(guid)
      .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
      .compose(user -> {
        ApiResponse<User> apiResponse = new ApiResponse<>(user);
        return Future.succeededFuture(apiResponse);
      });
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

    UserProvider userProvider = UserProvider.createFrom(vertx);
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
    Vertx vertx = routingContext.vertx();
    UserProvider userProvider = UserProvider.createFrom(vertx);
    return RealmProvider.createFrom(vertx)
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
