package net.bytle.tower.eraldy.api.implementer;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import jakarta.mail.internet.AddressException;
import net.bytle.email.BMailInternetAddress;
import net.bytle.exception.CastException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.UserApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.AuthScope;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.model.openapi.UserPostBody;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.tower.eraldy.objectProvider.UserProvider;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.FailureStatic;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.auth.AuthUser;

public class UserApiImpl implements UserApi {

  private final EraldyApiApp apiApp;

  public UserApiImpl(TowerApp towerApp) {
    this.apiApp = (EraldyApiApp) towerApp;
  }


  @Override
  public Future<ApiResponse<User>> userGet(RoutingContext routingContext, String userIdentifier, String realmIdentifier) {

    Future<Realm> realmFuture;
    UserProvider userProvider = apiApp.getUserProvider();
    RealmProvider realmProvider = apiApp.getRealmProvider();
    Guid userGuid = null;
    try {
      userGuid = userProvider.getGuidFromHash(userIdentifier);
      realmFuture = realmProvider.getRealmFromId(userGuid.getRealmOrOrganizationId());
    } catch (CastException e) {
      if (realmIdentifier == null) {
        return Future.failedFuture(
          TowerFailureException.builder()
            .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
            .setMessage("When the user identifier is not a guid, a realm identifier should be given")
            .build()
        );
      }
      realmFuture = this.apiApp.getRealmProvider()
        .getRealmFromIdentifier(realmIdentifier);
    }

    Guid finalUserGuid = userGuid;
    return realmFuture
      .compose(realm -> {
        if (realm == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_FOUND_404)
              .setMessage("The realm was not found")
              .build()
          );
        }
        return apiApp.getAuthProvider().checkRealmAuthorization(routingContext, realm, AuthScope.REALM_USER_GET);
      })
      .compose(realmChecked -> {
        Future<User> futureUser;
        if (finalUserGuid != null) {
          futureUser = userProvider.getUserById(
            finalUserGuid.validateRealmAndGetFirstObjectId(realmChecked.getLocalId()),
            realmChecked.getLocalId(),
            User.class,
            realmChecked
          );
        } else {
            BMailInternetAddress mailInternetAddress;
            try {
                mailInternetAddress = BMailInternetAddress.of(userIdentifier);
            } catch (AddressException e) {
                return Future.failedFuture(TowerFailureException
                  .builder()
                  .setMessage("The identifier is not a guid, nor an email ("+userIdentifier+")")
                  .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
                  .build()
                );
            }
            futureUser = userProvider.getUserByEmail(mailInternetAddress, realmChecked.getLocalId(),
            User.class,
            realmChecked);
        }
        return futureUser;
      })
      .compose(user -> {
        if (user == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_FOUND_404)
              .setMessage("The realm was found but not the user")
              .build()
          );
        }
        ApiResponse<User> apiResponse = new ApiResponse<>(user).setMapper(userProvider.getApiMapper());
        return Future.succeededFuture(apiResponse);
      });

  }

  @Override
  public Future<ApiResponse<User>> userGuidGet(RoutingContext routingContext, String guid) {
    return apiApp.getUserProvider()
      .getUserByGuid(guid, User.class, null)
      .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
      .compose(user -> {
        ApiResponse<User> apiResponse = new ApiResponse<>(user);
        return Future.succeededFuture(apiResponse);
      });
  }

  @Override
  public Future<ApiResponse<User>> userMeGet(RoutingContext routingContext) {

    AuthUser authSignedInUser;
    try {
      authSignedInUser = apiApp.getAuthProvider().getSignedInAuthUser(routingContext);
    } catch (NotFoundException e) {
      return Future.failedFuture(
        TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.NOT_LOGGED_IN_401)
          .build()
      );
    }
    return apiApp.getUserProvider()
      .getUserByGuid(authSignedInUser.getSubject(), User.class, null)
      .compose(user -> {
        ApiResponse<User> userApiResponse = new ApiResponse<>(user)
          .setMapper(apiApp.getUserProvider().getApiMapper());
        return Future.succeededFuture(userApiResponse);
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
        return userProvider.upsertUser(userRequested);
      })
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(userUpserted -> {
        userProvider.toPublicCloneWithoutRealm(userUpserted);
        return Future.succeededFuture(new ApiResponse<>(userUpserted));
      });

  }


}
