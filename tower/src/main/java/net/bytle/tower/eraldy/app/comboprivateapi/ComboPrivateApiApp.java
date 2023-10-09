package net.bytle.tower.eraldy.app.comboprivateapi;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.APIKeyHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import net.bytle.tower.TowerApexDomain;
import net.bytle.tower.TowerApp;
import net.bytle.tower.eraldy.app.OpenApiDoc;
import net.bytle.tower.eraldy.app.comboapp.ComboAppApp;
import net.bytle.tower.eraldy.app.comboprivateapi.openapi.invoker.ApiVertxSupport;
import net.bytle.tower.eraldy.auth.provider.ApiTokenAuthenticationProvider;

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
  public ComboPrivateApiApp openApiBindSecurityScheme(RouterBuilder builder, JsonObject jsonConfig) {

    /**
     * Configuring `AuthenticationHandler`s defined in the OpenAPI document
     * https://vertx.io/docs/vertx-web-openapi/java/#_configuring_authenticationhandlers_defined_in_the_openapi_document
     */
    ApiTokenAuthenticationProvider apiTokenAuthenticationProvider = new ApiTokenAuthenticationProvider(jsonConfig);
    builder
      .securityHandler(ApiTokenAuthenticationProvider.APIKEY_AUTH_SECURITY_SCHEME)
      .bindBlocking(config -> APIKeyHandler.create(apiTokenAuthenticationProvider));

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
  public String getPublicDefaultOperationPath() {
    return OpenApiDoc.DOC_OPERATION_PATH;
  }


  @Override
  protected String getPublicAbsolutePathMount() {
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
