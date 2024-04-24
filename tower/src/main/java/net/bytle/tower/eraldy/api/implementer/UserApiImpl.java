package net.bytle.tower.eraldy.api.implementer;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.ValidationException;
import net.bytle.exception.CastException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.UserApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.AuthUserScope;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.model.openapi.UserPostBody;
import net.bytle.tower.eraldy.module.realm.db.RealmProvider;
import net.bytle.tower.eraldy.module.user.db.UserProvider;
import net.bytle.tower.eraldy.module.user.inputs.UserInputProps;
import net.bytle.tower.eraldy.module.user.model.UserGuid;
import net.bytle.type.EmailAddress;
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
  public Future<ApiResponse<User>> userUserIdentifierGet(RoutingContext routingContext, String userIdentifier, String realmIdentifier) {
    Future<Realm> realmFuture;
    UserProvider userProvider = apiApp.getUserProvider();
    RealmProvider realmProvider = apiApp.getRealmProvider();
    UserGuid userGuid = null;
    try {
      userGuid = userProvider.getGuidFromHash(userIdentifier);
      realmFuture = realmProvider.getRealmFromLocalId(userGuid.getRealmId());
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

    UserGuid finalUserGuid = userGuid;
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
        return apiApp.getAuthProvider().checkRealmAuthorization(routingContext, realm, AuthUserScope.REALM_USER_GET);
      })
      .compose(realmChecked -> {
        Future<User> futureUser;
        if (finalUserGuid != null) {
          futureUser = userProvider.getUserByLocalId(
            finalUserGuid.getLocalId(),
            realmChecked
          );
        } else {
          EmailAddress mailInternetAddress;
          try {
            mailInternetAddress = EmailAddress.of(userIdentifier);
          } catch (CastException e) {
            return Future.failedFuture(TowerFailureException
              .builder()
              .setMessage("The identifier is not a guid, nor an email (" + userIdentifier + ")")
              .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
              .build()
            );
          }
          futureUser = userProvider.getUserByEmail(mailInternetAddress, realmChecked);
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

    String realmGuid = authSignedInUser.getRealmGuid();
    return apiApp.getRealmProvider()
      .getRealmFromIdentifier(realmGuid)
      .compose(realm -> {
        if (realm == null) {
          return Future.failedFuture(TowerFailureException.builder()
            .setMessage("The realm (" + realmGuid + ") was not found")
            .build()
          );
        }
        return apiApp.getUserProvider()
          .getUserByGuid(authSignedInUser.getSubject(), realm)
          .compose(user -> {
            ApiResponse<User> userApiResponse = new ApiResponse<>(user)
              .setMapper(apiApp.getUserProvider().getApiMapper());
            return Future.succeededFuture(userApiResponse);
          });
      });


  }

  @Override
  public Future<ApiResponse<User>> userPost(RoutingContext routingContext, UserPostBody userPostBody) {


    UserInputProps userRequested = new UserInputProps();
    userRequested.setEmailAddress(userPostBody.getUserEmail());
    userRequested.setGivenName(userPostBody.getUserName());
    userRequested.setFamilyName(userPostBody.getUserFullname());
    userRequested.setTitle(userPostBody.getUserTitle());
    userRequested.setAvatar(userPostBody.getUserAvatar());

    /**
     *
     */
    String userGuid = userPostBody.getUserGuid();
    String realmIdentifier = userPostBody.getRealmIdentifier();


    Future<Realm> realmFuture;
    UserGuid guid = null;
    UserProvider userProvider = this.apiApp.getUserProvider();
    if (userGuid == null) {
      if (realmIdentifier == null) {
        throw ValidationException.create("A realmIdentifier should be given for insertion or a userGuid for update", "realmHandle", null);
      }
      realmFuture = this.apiApp.getRealmProvider()
        .getRealmFromIdentifier(realmIdentifier);
    } else {

      try {
        guid = userProvider.getGuidFromHash(userGuid);
      } catch (CastException e) {
        return Future.failedFuture(new IllegalArgumentException("The user guid is not valid (" + userGuid + ")"));
      }

      long realmId = guid.getRealmId();

      realmFuture = this.apiApp
        .getRealmProvider()
        .getRealmFromLocalId(realmId);

    }


    UserGuid finalGuid = guid;
    return realmFuture
      .compose(realm -> {
        Future<User> futureUser = Future.succeededFuture();
        if (finalGuid != null) {
          long userIdFromGuid = finalGuid.getLocalId();
          futureUser = userProvider.getUserByLocalId(userIdFromGuid, realm);
        }
        return futureUser
          .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
          .compose(actualUser -> {
            if (actualUser == null) {
              return userProvider.insertUser(realm, userRequested);
            }
            return userProvider.updateUser(actualUser, userRequested);
          })
          .compose(userUpserted -> Future.succeededFuture(
            new ApiResponse<>(userUpserted)
              .setMapper(userProvider.getApiMapper()))
          );
      });
  }


}
