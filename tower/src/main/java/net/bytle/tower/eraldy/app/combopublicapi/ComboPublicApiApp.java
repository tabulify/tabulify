package net.bytle.tower.eraldy.app.combopublicapi;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.openapi.RouterBuilder;
import net.bytle.tower.eraldy.app.comboapp.ComboAppApp;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.invoker.ApiVertxSupport;
import net.bytle.vertx.*;

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
  public ComboPublicApiApp openApiBindSecurityScheme(RouterBuilder builder, ConfigAccessor configAccessor) {

    /**
     * Configuring `AuthenticationHandler`s defined in the OpenAPI document
     * https://vertx.io/docs/vertx-web-openapi/java/#_configuring_authenticationhandlers_defined_in_the_openapi_document
     */
    builder
      .securityHandler(OpenApiUtil.APIKEY_AUTH_SECURITY_SCHEME)
      .bindBlocking(config -> this.getApexDomain().getHttpServer().getApiKeyAuthenticator());
    builder
      .securityHandler(OpenApiUtil.BEARER_AUTH_SECURITY_SCHEME)
      .bindBlocking(config -> this.getApexDomain().getHttpServer().getBearerAuthenticator());

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
  public String getDefaultOperationPath() {
    return OpenApiDoc.DOC_OPERATION_PATH;
  }

  @Override
  public String getPathMount() {
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
