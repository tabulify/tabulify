package net.bytle.ip;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.openapi.RouterBuilder;
import net.bytle.ip.api.IpApiImpl;
import net.bytle.ip.handler.IpHandler;
import net.bytle.vertx.*;

public class IpApp extends TowerApp {


  public IpApp(HttpServer httpServer) {
    super(httpServer, EraldyDomain.getOrCreate(httpServer));
  }

  public static IpApp createForDomain(HttpServer httpServer) {
    return new IpApp(httpServer);
  }


  @Override
  public String getAppName() {
    return "ip";
  }

  @Override
  public TowerApp openApiMount(RouterBuilder builder) {
    new IpHandler(new IpApiImpl(this)).mount(builder);
    return this;
  }


  @Override
  public TowerApp openApiBindSecurityScheme(RouterBuilder builder, ConfigAccessor configAccessor) {

    /**
     * Only authentication via super token
     */
    builder
      .securityHandler(OpenApiSecurityNames.APIKEY_AUTH_SECURITY_SCHEME)
      .bindBlocking(config -> this.getHttpServer().getApiKeyAuthHandler());

    return this;
  }

  @Override
  protected String getPublicSubdomainName() {
    return "api";
  }

  @Override
  protected TowerApp addSpecificAppHandlers(Router router) {
    return this;
  }

  @Override
  public boolean hasOpenApiSpec() {
    return true;
  }

  /**
   * @return default is ipGet (ie /ip)
   */
  @Override
  public String getDefaultOperationPath() {
    return "/ip";
  }

  @Override
  public String getPathMount() {
    // mount at the root
    return "";
  }

  @Override
  public boolean getIsHtmlApp() {
    return false;
  }

  @Override
  public boolean isSocial() {
    return false;
  }

}
