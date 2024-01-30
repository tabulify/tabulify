package net.bytle.tower.eraldy.api.implementer;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.AuthClient;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.EnvApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.vertx.HttpRequestUtil;
import net.bytle.vertx.TowerApp;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class EnvApiImpl implements EnvApi {
  private final EraldyApiApp apiApp;

  public EnvApiImpl(TowerApp towerApp) {
    this.apiApp = (EraldyApiApp) towerApp;
  }

  @Override
  public Future<ApiResponse<Map<String, Object>>> envGet(RoutingContext routingContext) {

    HashMap<String, Object> jsonObject = new HashMap<>();


    /**
     * Http Request data
     */
    HashMap<String, Object> requestObject = new HashMap<>();
    jsonObject.put("request", requestObject);
    HttpServerRequest request = routingContext.request();
    requestObject.put("host", request.authority().host());
    requestObject.put("scheme", request.scheme());
    requestObject.put("path", request.path());
    requestObject.put("absoluteUri", routingContext.request().absoluteURI());

    /**
     * Request Remote Address
     */
    HashMap<String, Object> remoteAddressObject = new HashMap<>();
    SocketAddress remoteAddress = routingContext.request().remoteAddress();
    remoteAddressObject.put("host", remoteAddress.host());
    remoteAddressObject.put("hostAddress", remoteAddress.hostAddress());
    remoteAddressObject.put("port", remoteAddress.port());
    requestObject.put("remoteAddress", remoteAddressObject);

    /**
     * Local Address
     */
    HashMap<String, Object> localAddressObject = new HashMap<>();
    SocketAddress localAddress = routingContext.request().localAddress();
    localAddressObject.put("host", localAddress.host());
    localAddressObject.put("hostAddress", localAddress.hostAddress());
    localAddressObject.put("port", localAddress.port());
    requestObject.put("localAddress", localAddressObject);

    /**
     * Calculated
     */
    HashMap<String, Object> calculatedObject = new HashMap<>();
    calculatedObject.put("remoteHost", HttpRequestUtil.getRemoteHost(routingContext));
    calculatedObject.put("remoteScheme", HttpRequestUtil.getRemoteScheme(routingContext));
    AuthClient realm = this.apiApp.getAuthClientHandler().getApiClientStoredOnContext(routingContext);
    calculatedObject.put("authRealmHandle", realm.getApp().getRealm().getHandle());
    requestObject.put("calculated", calculatedObject);

    /**
     * System environment variable
     */
    if (routingContext.user() != null) {
      HashMap<String, Object> systemObject = new HashMap<>();
      jsonObject.put("system", systemObject);
      String currentPath = Paths.get(".").toAbsolutePath().toString();
      systemObject.put("pwd", currentPath);
    }

    return Future.succeededFuture(new ApiResponse<>(jsonObject));

  }
}
