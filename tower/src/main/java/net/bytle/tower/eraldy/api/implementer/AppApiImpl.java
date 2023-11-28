package net.bytle.tower.eraldy.api.implementer;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.ValidationException;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.AppApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.AuthScope;
import net.bytle.tower.eraldy.mixin.AppPublicMixinWithRealm;
import net.bytle.tower.eraldy.mixin.RealmPublicMixin;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.model.openapi.App;
import net.bytle.tower.eraldy.model.openapi.AppPostBody;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.objectProvider.AppProvider;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.FailureStatic;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;

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
    return this.apiApp.getRealmProvider()
      .getRealmFromIdentifier(appPostBody.getRealmIdentifier(), Realm.class)
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(realm -> appProvider.postApp(appPostBody))
      .compose(app -> {
        appProvider.toPublicClone(app);
        return Future.succeededFuture(new ApiResponse<>(app));
      });

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
