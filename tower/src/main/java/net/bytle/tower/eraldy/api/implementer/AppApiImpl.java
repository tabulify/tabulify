package net.bytle.tower.eraldy.api.implementer;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.ValidationException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.AppApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.App;
import net.bytle.tower.eraldy.model.openapi.AppPostBody;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.objectProvider.AppProvider;
import net.bytle.vertx.FailureStatic;
import net.bytle.vertx.TowerApp;

import java.net.URI;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class AppApiImpl implements AppApi {

  private EraldyApiApp apiApp;

  @SuppressWarnings("unused")
  public AppApiImpl(TowerApp towerApp) {
    this.apiApp = (EraldyApiApp) towerApp;
  }

  @Override
  public Future<ApiResponse<App>> appGet(RoutingContext routingContext, String appGuid, String appUri, String realmHandle, String realmGuid) {


    Vertx vertx = routingContext.vertx();
    AppProvider appProvider = apiApp.getAppProvider();
    Future<App> futureApp;
    if (appGuid != null) {
      futureApp = appProvider.getAppByGuid(appGuid);
    } else {
      if (appUri == null) {
        throw ValidationException.create("A appGuid or appUri should be given", "appGuid", null);
      }
      if (realmHandle == null && realmGuid == null) {
        throw ValidationException.create("With an appUri, the realmHandle should be given", "realmHandle", null);
      }
      URI appUriAsUri;
      try {
        appUriAsUri = URI.create(appUri);
      } catch (Exception e) {
        throw ValidationException.create("The appUri is not a valid uri", "appUri", appUri);
      }
      futureApp = this.apiApp.getRealmProvider()
        .getRealmFromGuidOrHandle(realmGuid, realmHandle, Realm.class)
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
    if (appPostBody.getRealmGuid() == null && appPostBody.getRealmHandle() == null) {
      throw ValidationException.create("A realmGuid or realmHandle should be given", "realmHandle", null);
    }
    Vertx vertx = routingContext.vertx();
    AppProvider appProvider = apiApp.getAppProvider();
    return this.apiApp.getRealmProvider()
      .getRealmFromGuidOrHandle(appPostBody.getRealmGuid(), appPostBody.getRealmHandle(), Realm.class)
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(realm -> appProvider.postApp(appPostBody))
      .compose(app -> {
        appProvider.toPublicClone(app);
        return Future.succeededFuture(new ApiResponse<>(app));
      });

  }


  @Override
  public Future<ApiResponse<java.util.List<App>>> appsGet(RoutingContext routingContext, String realmGuid, String
    realmHandle) {
    if (realmGuid == null && realmHandle == null) {
      throw ValidationException.create("The realm guid or handle should be given", "realmGuid", null);
    }
    Vertx vertx = routingContext.vertx();
    AppProvider appProvider = apiApp.getAppProvider();
    return this.apiApp.getRealmProvider()
      .getRealmFromGuidOrHandle(realmGuid, realmHandle, Realm.class)
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(appProvider::getApps)
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(apps -> {
          java.util.List<App> publicApps = apps.stream().map(appProvider::toPublicClone)
            .collect(Collectors.toList());
          return Future.succeededFuture(new ApiResponse<>(publicApps));
        }
      );
  }

}
