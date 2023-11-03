package net.bytle.tower.eraldy.api;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.openapi.RouterBuilder;
import net.bytle.tower.eraldy.api.implementer.callback.ListRegistrationEmailCallback;
import net.bytle.tower.eraldy.api.implementer.callback.PasswordResetEmailCallback;
import net.bytle.tower.eraldy.api.implementer.callback.UserLoginEmailCallback;
import net.bytle.tower.eraldy.api.implementer.callback.UserRegisterEmailCallback;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiVertxSupport;
import net.bytle.tower.util.OAuthExternal;
import net.bytle.tower.util.OAuthQueryProperty;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.*;

/**
 * The public api
 */
public class EraldyApiApp extends TowerApp {



  public EraldyApiApp(TowerApexDomain topLevelDomain) {
    super(topLevelDomain);
  }



  public static TowerApp create(TowerApexDomain topLevelDomain) {

    return new EraldyApiApp(topLevelDomain);

  }

  @Override
  public String getAppName() {
    return "Api";
  }


  @Override
  public EraldyApiApp openApiMount(RouterBuilder builder) {
    ApiVertxSupport.mount(builder, this);
    return this;
  }

  @Override
  public EraldyApiApp openApiBindSecurityScheme(RouterBuilder builder, ConfigAccessor configAccessor) {

    /**
     * Configuring `AuthenticationHandler`s defined in the OpenAPI document
     * https://vertx.io/docs/vertx-web-openapi/java/#_configuring_authenticationhandlers_defined_in_the_openapi_document
     */
    builder
      .securityHandler(OpenApiUtil.APIKEY_AUTH_SECURITY_SCHEME)
      .bindBlocking(config -> this.getApexDomain().getHttpServer().getApiKeyHandler());
    builder
      .securityHandler(OpenApiUtil.BEARER_AUTH_SECURITY_SCHEME)
      .bindBlocking(config -> this.getApexDomain().getHttpServer().getBearerAuthenticationHandler());

    return this;

  }

  @Override
  protected String getPublicSubdomainName() {
    return "api";
  }

  @Override
  protected TowerApp addSpecificAppHandlers(Router router) {

    /**
     * Add the external OAuths
     */
    OAuthExternal.build(this, router);

    /**
     * Add the registration validation callback
     */
    getUserRegistrationValidation()
      .addCallback(router);

    /**
     * Add the email login validation callback
     */
    getUserEmailLoginCallback()
      .addCallback(router);

    /**
     * Add the password reset callback
     */
    getPasswordResetCallback()
      .addCallback(router);

    /**
     * Add the user list registration callback
     */
    getUserListRegistrationCallback()
      .addCallback(router);

    return this;
  }

  public UserLoginEmailCallback getUserEmailLoginCallback() {
    return UserLoginEmailCallback.getOrCreate(this);
  }

  public ListRegistrationEmailCallback getUserListRegistrationCallback() {
    return ListRegistrationEmailCallback.getOrCreate(this);
  }

  /**
   * @return the registration validation manager
   */
  public UserRegisterEmailCallback getUserRegistrationValidation() {
    return UserRegisterEmailCallback.getOrCreate(this);
  }

  public PasswordResetEmailCallback getPasswordResetCallback() {
    return PasswordResetEmailCallback.getOrCreate(this);
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

  /**
   * @return the login uri
   */
  public UriEnhanced getLoginUriForEraldyRealm(String redirectUri) {
    return this.getPublicRequestUriForOperationPath("/login")
      .addQueryProperty(OAuthQueryProperty.REDIRECT_URI, redirectUri)
      .addQueryProperty(OAuthQueryProperty.REALM_HANDLE, this.getApexDomain().getRealmHandle());
  }

  public UriEnhanced getLogoutUriForEraldyRealm(String redirectUri) {
    return this.getPublicRequestUriForOperationPath("/logout")
      .addQueryProperty(OAuthQueryProperty.REDIRECT_URI, redirectUri)
      .addQueryProperty(OAuthQueryProperty.REALM_HANDLE, this.getApexDomain().getRealmHandle());
  }

}
