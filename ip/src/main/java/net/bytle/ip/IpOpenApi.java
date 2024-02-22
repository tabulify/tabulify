package net.bytle.ip;

import io.vertx.ext.web.openapi.RouterBuilder;
import net.bytle.ip.api.IpApiImpl;
import net.bytle.ip.handler.IpHandler;
import net.bytle.vertx.OpenApiInstance;
import net.bytle.vertx.OpenApiSecurityNames;
import net.bytle.vertx.OpenApiService;
import net.bytle.vertx.TowerApp;

public class IpOpenApi implements OpenApiInstance {

  private final IpApp ipApp;

  public IpOpenApi(IpApp ipApp) {
    this.ipApp = ipApp;
  }

  @Override
  public OpenApiInstance openApiMount(RouterBuilder builder) {
    new IpHandler(new IpApiImpl(ipApp)).mount(builder);
    return this;
  }

  @Override
  public OpenApiInstance openApiAddSecurityHandlers(RouterBuilder routerBuilder, OpenApiService openApiService) {
    /**
     * Only authentication via super token
     */
    routerBuilder
      .securityHandler(OpenApiSecurityNames.APIKEY_AUTH_SECURITY_SCHEME)
      .bindBlocking(config -> this.ipApp.getHttpServer().getApiKeyAuthHandler());

    return this;
  }

  @Override
  public boolean requireSecurityHandlers() {
    return true;
  }

  @Override
  public TowerApp getApp() {
    return this.ipApp;
  }

}
