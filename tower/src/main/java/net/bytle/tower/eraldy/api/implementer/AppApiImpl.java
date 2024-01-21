package net.bytle.tower.eraldy.api.implementer;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.ValidationException;
import net.bytle.exception.CastException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.AppApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.AuthScope;
import net.bytle.tower.eraldy.mixin.AppPublicMixinWithRealm;
import net.bytle.tower.eraldy.mixin.RealmPublicMixin;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.objectProvider.AppProvider;
import net.bytle.tower.eraldy.objectProvider.AuthProvider;
import net.bytle.tower.eraldy.objectProvider.ListProvider;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.*;

public class AppApiImpl implements AppApi {

  private final EraldyApiApp apiApp;
  private final JsonMapper apiMapper;

  public AppApiImpl(TowerApp towerApp) {
    this.apiApp = (EraldyApiApp) towerApp;
    this.apiMapper = apiApp.getApexDomain().getHttpServer().getServer().getJacksonMapperManager()
      .jsonMapperBuilder()
      .addMixIn(App.class, AppPublicMixinWithRealm.class)
      .addMixIn(User.class, UserPublicMixinWithoutRealm.class)
      .addMixIn(Realm.class, RealmPublicMixin.class)
      .build();
  }

  @Override
  public Future<ApiResponse<ListItem>> appAppListPost(RoutingContext routingContext, String appIdentifier, ListBody listBody, String realmIdentifier) {

    RoutingContextWrapper routingContextWrapper = RoutingContextWrapper.createFrom(routingContext);
    try {
      appIdentifier = routingContextWrapper.getRequestPathParameter("appIdentifier").getString();
    } catch (NotFoundException e) {
      throw ValidationException.create("An app identifier (handle or guid) should be given", "appIdentifier", null);
    }
    realmIdentifier = routingContextWrapper.getRequestQueryParameterAsString("realmIdentifier");
    Future<Realm> realmFuture;
    Guid guid = null;
    try {
      guid = this.apiApp.getAppProvider().getGuid(appIdentifier);
      realmFuture = this.apiApp.getRealmProvider().getRealmFromId(guid.getRealmOrOrganizationId());
    } catch (CastException e) {
      if (realmIdentifier == null) {
        throw ValidationException.create("An realm identifier (handle or guid) should be given when the app identifier (" + appIdentifier + ") is a handle", "realmIdentifier", null);
      }
      realmFuture = this.apiApp.getRealmProvider().getRealmFromIdentifier(realmIdentifier);
    }

    Guid finalGuid = guid;
    String finalAppIdentifier = appIdentifier;
    String finalRealmIdentifier = realmIdentifier;
    return realmFuture
      .compose(realm -> {

        if (realm == null) {
          String message;
          if (finalGuid != null) {
            message = "The realm for the app (" + finalAppIdentifier + ") was not found";
          } else {
            message = "The realm (" + finalRealmIdentifier + ") was not found";
          }
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_FOUND_404)
              .setMessage(message)
              .build()
          );
        }

        return apiApp.getAuthProvider().checkRealmAuthorization(routingContext, realm, AuthScope.LIST_CREATION);
      })
      .compose(futureRealm -> {
        if (finalGuid != null) {
          return this.apiApp.getAppProvider().getAppById(finalGuid.validateRealmAndGetFirstObjectId(futureRealm.getLocalId()), futureRealm);
        }
        return this.apiApp.getAppProvider().getAppByHandle(finalAppIdentifier, futureRealm);
      })
      .compose(futureApp -> {
        ListProvider listProvider = apiApp.getListProvider();
        return listProvider
          .postList(listBody, futureApp)
          .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
          .compose(publication -> Future.succeededFuture(new ApiResponse<>(publication).setMapper(listProvider.getApiMapper())));
      });


  }

  @Override
  public Future<ApiResponse<App>> appGet(RoutingContext routingContext, String appIdentifier, String realmIdentifier) {


    Future<Realm> futureRealm;
    Guid appGuid = null;
    try {
      appGuid = this.apiApp.getAppProvider().getGuid(appIdentifier);
      futureRealm = this.apiApp.getRealmProvider().getRealmFromId(appGuid.getRealmOrOrganizationId());
    } catch (CastException e) {
      if (realmIdentifier == null) {
        return Future.failedFuture(
          TowerFailureException
            .builder()
            .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
            .setMessage("The realm identifier cannot be null with the handle appIdentifier (" + appIdentifier + ")")
            .buildWithContextFailing(routingContext)
        );
      }
      futureRealm = this.apiApp.getRealmProvider().getRealmFromIdentifier(realmIdentifier);
    }

    Guid finalAppGuid = appGuid;
    return futureRealm.compose(realm -> {
        if (realm == null) {
          String message;
          if (finalAppGuid != null) {
            message = "The realm of the app (" + appIdentifier + ") was not found";
          } else {
            message = "The realm (" + realmIdentifier + ") was not found";
          }
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_FOUND_404)
              .setMessage(message)
              .build()
          );
        }
        return this.apiApp.getAuthProvider().checkRealmAuthorization(routingContext, realm, AuthScope.REALM_APP_GET);
      }).compose(realm -> {
        Future<App> futureApp;
        if (finalAppGuid != null) {
          futureApp = this.apiApp.getAppProvider().getAppById(finalAppGuid.validateRealmAndGetFirstObjectId(realm.getLocalId()), realm);
        } else {
          futureApp = this.apiApp.getAppProvider().getAppByHandle(appIdentifier, realm);
        }
        return futureApp;
      })
      .compose(app -> {
        if (app == null) {
          String message;
          if (finalAppGuid != null) {
            message = "The realm was found but not the app (" + appIdentifier + ")";
          } else {
            message = "The realm (" + realmIdentifier + ") was found but not the app (" + appIdentifier + ")";
          }
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_FOUND_404)
              .setMessage(message)
              .build()
          );
        }
        return Future.succeededFuture(new ApiResponse<>(app)
          .setMapper(this.apiMapper));
      });
  }


  @Override
  public Future<ApiResponse<App>> appPost(RoutingContext routingContext, AppPostBody appPostBody) {

    /**
     * Important to catch it now to show that
     * is a validation exception and not an internal error.
     */
    if (appPostBody.getRealmIdentifier() == null) {
      throw ValidationException.create("A realm identifier should be given", "realmIdentifier", null);
    }
    AppProvider appProvider = apiApp.getAppProvider();
    AuthProvider authProvider = apiApp.getAuthProvider();
    return this.apiApp.getRealmProvider()
      .getRealmFromIdentifier(appPostBody.getRealmIdentifier(), Realm.class)
      .compose(realm -> {
        if (realm == null) {
          return Future.failedFuture(
            TowerFailureException
              .builder()
              .setMessage("The realm (" + appPostBody.getRealmIdentifier() + ") was not found")
              .setType(TowerFailureTypeEnum.NOT_FOUND_404)
              .build()
          );
        }
        return authProvider.checkRealmAuthorization(routingContext, realm, AuthScope.APP_CREATE);
      })
      .compose(realm -> appProvider.postApp(appPostBody, realm))
      .compose(app -> Future.succeededFuture(new ApiResponse<>(app).setMapper(appProvider.getApiMapper())));

  }


  @Override
  public Future<ApiResponse<java.util.List<App>>> appsGet(RoutingContext routingContext, String realmIdentifier) {

    if (realmIdentifier == null) {
      throw ValidationException.create("A realm identifier should be given", "realmIdentifier", null);
    }


    return this.apiApp.getRealmProvider()
      .getRealmFromIdentifierNotNull(realmIdentifier, Realm.class)
      .compose(realm -> this.apiApp.getAuthProvider().checkRealmAuthorization(routingContext, realm, AuthScope.REALM_APPS_GET))
      .compose(
        realm -> apiApp.getAppProvider().getApps(realm),
        err -> Future.failedFuture(
          TowerFailureException.builder()
            .setMessage("Unable to get the realm with the identifier (" + realmIdentifier + ")")
            .setCauseException(err)
            .buildWithContextFailing(routingContext)
        ))
      .compose(
        apps -> Future.succeededFuture(new ApiResponse<>(apps).setMapper(this.apiMapper)),
        err -> Future.failedFuture(
          TowerFailureException.builder()
            .setMessage("Unable to get the apps for the realm (" + realmIdentifier + ")")
            .setCauseException(err)
            .buildWithContextFailing(routingContext)
        )
      );
  }

}
