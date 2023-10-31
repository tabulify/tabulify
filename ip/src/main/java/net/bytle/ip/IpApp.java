package net.bytle.ip;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.openapi.RouterBuilder;
import net.bytle.ip.api.IpApiImpl;
import net.bytle.ip.handler.IpHandler;
import net.bytle.vertx.ConfigAccessor;
import net.bytle.vertx.EraldyDomain;
import net.bytle.vertx.TowerApexDomain;
import net.bytle.vertx.TowerApp;

public class IpApp extends TowerApp {

  public IpApp(TowerApexDomain towerApexDomain) {
    super(towerApexDomain);
  }

  public static TowerApp createForDomain(EraldyDomain eraldyDomain) {
    return new IpApp(eraldyDomain);
  }

  @Override
  public String getAppName() {
    return "ip";
  }

  @Override
  public TowerApp openApiMount(RouterBuilder builder) {
    new IpHandler(new IpApiImpl()).mount(builder);
    return this;
  }

  @Override
  public TowerApp openApiBindSecurityScheme(RouterBuilder builder, ConfigAccessor configAccessor) {
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

  @Override
  public String getPublicDefaultOperationPath() {
    return "/ip";
  }

  @Override
  protected String getPublicAbsolutePathMount() {
    return "/ip";
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
