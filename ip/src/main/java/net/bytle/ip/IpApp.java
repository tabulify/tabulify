package net.bytle.ip;

import net.bytle.vertx.EraldyDomain;
import net.bytle.vertx.HttpServer;
import net.bytle.vertx.OpenApiService;
import net.bytle.vertx.TowerApp;

public class IpApp extends TowerApp {


  public IpApp(HttpServer httpServer) {
    super(httpServer, EraldyDomain.getOrCreate(httpServer));
    new OpenApiService(new IpOpenApi(this));
  }

  public static IpApp createForDomain(HttpServer httpServer) {
    return new IpApp(httpServer);
  }


  @Override
  public String getAppName() {
    return "ip";
  }


  @Override
  protected String getPublicSubdomainName() {
    return "api";
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
