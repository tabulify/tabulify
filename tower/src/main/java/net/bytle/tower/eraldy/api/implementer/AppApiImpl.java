package net.bytle.tower.eraldy.api.implementer;

import com.fasterxml.jackson.databind.json.JsonMapper;
import graphql.schema.DataFetchingEnvironment;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.ValidationException;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.AppApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.AuthUserScope;
import net.bytle.tower.eraldy.mixin.AppPublicMixinWithRealm;
import net.bytle.tower.eraldy.mixin.RealmPublicMixin;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.module.app.model.AppGuid;
import net.bytle.tower.eraldy.module.list.db.ListProvider;
import net.bytle.tower.eraldy.objectProvider.AppProvider;
import net.bytle.tower.eraldy.objectProvider.AuthProvider;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;

import java.util.List;

public class AppApiImpl implements AppApi {

  private final EraldyApiApp apiApp;
  private final JsonMapper apiMapper;

  public AppApiImpl(TowerApp towerApp) {
    this.apiApp = (EraldyApiApp) towerApp;
    this.apiMapper = apiApp.getHttpServer().getServer().getJacksonMapperManager()
      .jsonMapperBuilder()
      .addMixIn(App.class, AppPublicMixinWithRealm.class)
      .addMixIn(User.class, UserPublicMixinWithoutRealm.class)
      .addMixIn(Realm.class, RealmPublicMixin.class)
      .build();
  }

  @Override
  public Future<ApiResponse<List<ListObject>>> appAppIdentifierListsGet(RoutingContext routingContext, String appIdentifier) {

    ListProvider listProvider = this.apiApp.getListProvider();
    AppGuid guid;
    try {
      guid = this.apiApp.getAppProvider().getGuidFromHash(appIdentifier);
    } catch (CastException e) {
      return Future.failedFuture(TowerFailureException.builder()
        .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
        .setMessage("The app identifier ("+appIdentifier+") should be a app guid, an handle is not yet supported")
        .build()
      );
    }
    return this.apiApp.getRealmProvider()
      .getRealmFromLocalId(guid.getRealmId())
      .compose(realmRes -> this.apiApp.getAuthProvider().checkRealmAuthorization(routingContext, realmRes, AuthUserScope.APP_LISTS_GET))
      .compose(realmAfterCheck -> this.apiApp.getAppProvider().getAppByIdentifier(appIdentifier, realmAfterCheck))
      .compose(listProvider::getListsForApp)
      .compose(lists -> Future.succeededFuture(new ApiResponse<>(lists).setMapper(listProvider.getApiMapper())));


  }


  @Override
  public Future<ApiResponse<ListObject>> appAppListPost(RoutingContext routingContext, String appIdentifier, ListBody listBody, String realmIdentifier) {

    /**
     * See {@link net.bytle.tower.eraldy.module.list.graphql.ListGraphQLImpl#createList(DataFetchingEnvironment)}
     */
    return Future.failedFuture(new InternalException("Deprecated for the graphql createList mutation"));


  }

  @Override
  public Future<ApiResponse<App>> appGet(RoutingContext routingContext, String appIdentifier, String realmIdentifier) {


    Future<Realm> futureRealm;
    AppGuid appGuid = null;
    try {
      appGuid = this.apiApp.getHttpServer().getServer().getJacksonMapperManager().getDeserializer(AppGuid.class).deserialize(appIdentifier);
      futureRealm = this.apiApp.getRealmProvider().getRealmFromLocalId(appGuid.getRealmId());
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

    AppGuid finalAppGuid = appGuid;
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
        return this.apiApp.getAuthProvider().checkRealmAuthorization(routingContext, realm, AuthUserScope.REALM_APP_GET);
      }).compose(realm -> {
        Future<App> futureApp;
        if (finalAppGuid != null) {
          futureApp = this.apiApp.getAppProvider().getAppById(finalAppGuid.getAppLocalId(realm.getLocalId()), realm);
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
      .getRealmFromIdentifier(appPostBody.getRealmIdentifier())
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
        return authProvider.checkRealmAuthorization(routingContext, realm, AuthUserScope.APP_CREATE);
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
      .getRealmFromIdentifierNotNull(realmIdentifier)
      .compose(realm -> this.apiApp.getAuthProvider().checkRealmAuthorization(routingContext, realm, AuthUserScope.REALM_APPS_GET))
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
