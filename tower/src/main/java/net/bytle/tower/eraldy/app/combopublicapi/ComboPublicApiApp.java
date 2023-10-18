package net.bytle.tower.eraldy.app.combopublicapi;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.APIKeyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import net.bytle.tower.eraldy.app.comboapp.ComboAppApp;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.invoker.ApiVertxSupport;
import net.bytle.tower.eraldy.auth.provider.ApiTokenAuthenticationProvider;
import net.bytle.tower.util.JwtAuthManager;
import net.bytle.vertx.OpenApiDoc;
import net.bytle.vertx.TowerApexDomain;
import net.bytle.vertx.TowerApp;

/**
 * The public api
 */
public class ComboPublicApiApp extends TowerApp {

  private static ComboPublicApiApp publicApi;

  public ComboPublicApiApp(TowerApexDomain topLevelDomain) {
    super(topLevelDomain);
  }

  public static TowerApp get() {

    return publicApi;

  }

  public static TowerApp create(TowerApexDomain topLevelDomain) {

    publicApi = new ComboPublicApiApp(topLevelDomain);
    return publicApi;

  }

  @Override
  public String getAppName() {
    return "combo-public";
  }


  @Override
  public ComboPublicApiApp openApiMount(RouterBuilder builder) {
    ApiVertxSupport.mount(builder);
    return this;
  }

  @Override
  public ComboPublicApiApp openApiBindSecurityScheme(RouterBuilder builder, JsonObject jsonConfig) {

    /**
     * Configuring `AuthenticationHandler`s defined in the OpenAPI document
     * https://vertx.io/docs/vertx-web-openapi/java/#_configuring_authenticationhandlers_defined_in_the_openapi_document
     */
    ApiTokenAuthenticationProvider apiTokenAuthenticationProvider = new ApiTokenAuthenticationProvider(jsonConfig);
    builder
      .securityHandler(ApiTokenAuthenticationProvider.APIKEY_AUTH_SECURITY_SCHEME)
      .bindBlocking(config -> APIKeyHandler.create(apiTokenAuthenticationProvider));
    builder
      .securityHandler(ApiTokenAuthenticationProvider.BEARER_AUTH_SECURITY_SCHEME)
      .bindBlocking(config -> JWTAuthHandler.create(JwtAuthManager.get().getProvider()));

    return this;

  }

  @Override
  protected String getPublicSubdomainName() {
    return "api";
  }

  @Override
  protected TowerApp addSpecificAppHandlers(Router router) {
    // no specific handlers to add
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
    return "/"+ ComboAppApp.COMBO_NAME;
  }

  @Override
  public boolean getIsHtmlApp() {
    return false;
  }

  @Override
  public boolean isSocial() {
    return true;
  }

}
