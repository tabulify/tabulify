package net.bytle.tower.eraldy.app.comboprivateapi;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.openapi.RouterBuilder;
import net.bytle.tower.eraldy.app.comboapp.ComboAppApp;
import net.bytle.tower.eraldy.app.comboprivateapi.openapi.invoker.ApiVertxSupport;
import net.bytle.vertx.*;

/**
 * The combo private api app
 */
public class ComboPrivateApiApp extends TowerApp {

  private static ComboPrivateApiApp privateApi;

  public ComboPrivateApiApp(TowerApexDomain towerApexDomain) {
    super(towerApexDomain);
  }


  public static TowerApp create(TowerApexDomain towerApexDomain) {

    privateApi = new ComboPrivateApiApp(towerApexDomain);
    return privateApi;

  }

  public static ComboPrivateApiApp get() {
    return privateApi;
  }


  @Override
  public String getAppName() {
    return "combo-private";
  }


  @Override
  public ComboPrivateApiApp openApiMount(RouterBuilder builder) {
    ApiVertxSupport.mount(builder);
    return this;
  }

  @Override
  public ComboPrivateApiApp openApiBindSecurityScheme(RouterBuilder builder, ConfigAccessor jsonConfig) {


    builder
      .securityHandler(OpenApiUtil.APIKEY_AUTH_SECURITY_SCHEME)
      .bindBlocking(config -> this.getApexDomain().getHttpServer().getApiKeyHandler());

    return this;

  }

  @Override
  protected String getPublicSubdomainName() {
    return "api";
  }

  @Override
  protected TowerApp addSpecificAppHandlers(Router router) {
    // no handlers to add
    return this;
  }

  @Override
  public boolean hasOpenApiSpec() {
    return true;
  }

  @Override
  public String getDefaultOperationPath() {
    return OpenApiDoc.DOC_OPERATION_PATH;
  }


  @Override
  public String getPathMount() {
    return "/" + ComboAppApp.COMBO_NAME + "/_private";
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
