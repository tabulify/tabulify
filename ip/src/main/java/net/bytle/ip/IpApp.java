package net.bytle.ip;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import net.bytle.exception.InternalException;
import net.bytle.ip.api.IpApiImpl;
import net.bytle.ip.handler.IpHandler;
import net.bytle.vertx.*;

public class IpApp extends TowerApp {


  public IpApp(TowerApexDomain towerApexDomain) {
    super(towerApexDomain);
  }

  public static IpApp createForDomain(EraldyDomain eraldyDomain) {
    return new IpApp(eraldyDomain);
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

    AuthenticationHandler tokenAuthenticator = this.getApexDomain().getHttpServer().getBearerAuthenticator();
    if (tokenAuthenticator == null) {
      throw new InternalException("The bearer authenticator should be added to the HTTP server");
    }
    builder
      .securityHandler(OpenApiUtil.BEARER_AUTH_SECURITY_SCHEME)
      .bindBlocking(config -> tokenAuthenticator);

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
