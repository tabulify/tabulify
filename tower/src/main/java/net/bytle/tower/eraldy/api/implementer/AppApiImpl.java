package net.bytle.tower.eraldy.api.implementer;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.ValidationException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.AppApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.mixin.AppPublicMixinWithRealm;
import net.bytle.tower.eraldy.mixin.RealmPublicMixin;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.model.openapi.App;
import net.bytle.tower.eraldy.model.openapi.AppPostBody;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.objectProvider.AppProvider;
import net.bytle.vertx.FailureStatic;
import net.bytle.vertx.TowerApp;

import java.net.URI;
import java.util.NoSuchElementException;

public class AppApiImpl implements AppApi {

  private final EraldyApiApp apiApp;
  private final JsonMapper jsonMapper;

  public AppApiImpl(TowerApp towerApp) {
    this.apiApp = (EraldyApiApp) towerApp;
    this.jsonMapper = apiApp.getApexDomain().getHttpServer().getServer().getJacksonMapperManager()
      .jsonMapperBuilder()
      .addMixIn(App.class, AppPublicMixinWithRealm.class)
      .addMixIn(User.class, UserPublicMixinWithoutRealm.class)
      .addMixIn(Realm.class, RealmPublicMixin.class)
      .build();
  }

  @Override
  public Future<ApiResponse<App>> appGet(RoutingContext routingContext, String appGuid, String appUri, String realmIdentifier) {

    AppProvider appProvider = apiApp.getAppProvider();
    Future<App> futureApp;
    if (appGuid != null) {
      futureApp = appProvider.getAppByGuid(appGuid);
    } else {
      if (appUri == null) {
        throw ValidationException.create("A appGuid or appUri should be given", "appGuid", null);
      }
      if (realmIdentifier == null) {
        throw ValidationException.create("With an appUri, a realm identifier should be given", "realmIdentifier", null);
      }
      URI appUriAsUri;
      try {
        appUriAsUri = URI.create(appUri);
      } catch (Exception e) {
        throw ValidationException.create("The appUri is not a valid uri", "appUri", appUri);
      }
      futureApp = this.apiApp.getRealmProvider()
        .getRealmFromIdentifier(realmIdentifier, Realm.class)
        .compose(realm -> appProvider.getAppByUri(appUriAsUri, realm));
    }
    return futureApp
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(app -> {
        if (app == null) {
          throw new NoSuchElementException("No app was found");
        }
        appProvider.toPublicClone(app);
        return Future.succeededFuture(new ApiResponse<>(app));
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
      .compose(realm -> appProvider.postApp(appPostBody, routingContext))
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
    AppProvider appProvider = apiApp.getAppProvider();
    return this.apiApp.getRealmProvider()
      .getRealmFromIdentifier(realmIdentifier, Realm.class)
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(realm -> {
        this.apiApp.getAuthSignedInUser()
        return appProvider.getApps(realm);
      })
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(apps -> Future.succeededFuture(new ApiResponse<>(apps)
          .setMapper(this.jsonMapper)
        )
      );
  }

}
