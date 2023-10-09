package net.bytle.tower.eraldy.app.memberapp;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.openapi.RouterBuilder;
import net.bytle.tower.TowerApexDomain;
import net.bytle.tower.TowerApp;
import net.bytle.tower.eraldy.app.memberapp.implementer.callback.ListRegistrationEmailCallback;
import net.bytle.tower.eraldy.app.memberapp.implementer.callback.PasswordResetEmailCallback;
import net.bytle.tower.eraldy.app.memberapp.implementer.callback.UserLoginEmailCallback;
import net.bytle.tower.eraldy.app.memberapp.implementer.callback.UserRegisterEmailCallback;
import net.bytle.tower.eraldy.app.memberapp.openapi.invoker.ApiVertxSupport;
import net.bytle.tower.util.OAuthExternal;
import net.bytle.tower.util.OAuthQueryProperty;
import net.bytle.type.UriEnhanced;


public class EraldyMemberApp extends TowerApp {

  private static EraldyMemberApp memberApp;

  public EraldyMemberApp(TowerApexDomain topLevelDomain) {
    super(topLevelDomain);
  }

  public static EraldyMemberApp create(TowerApexDomain topLevelDomain) {

    memberApp = new EraldyMemberApp(topLevelDomain);
    return memberApp;

  }

  public static EraldyMemberApp get() {

    return memberApp;

  }


  @Override
  public String getAppName() {
    return "member";
  }


  @Override
  public EraldyMemberApp openApiMount(RouterBuilder builder) {
    ApiVertxSupport.mount(builder);
    return this;
  }

  @Override
  public EraldyMemberApp openApiBindSecurityScheme(RouterBuilder builder, JsonObject jsonConfig) {
    // no security scheme
    return this;
  }

  @Override
  protected String getPublicSubdomainName() {
    return "member";
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


  @Override
  public boolean hasOpenApiSpec() {
    return true;
  }

  @Override
  public String getPublicDefaultOperationPath() {
    return "/login";
  }

  @Override
  protected String getPublicAbsolutePathMount() {
    return "";
  }

  @Override
  public boolean getIsHtmlApp() {
    return true;
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

  public PasswordResetEmailCallback getPasswordResetCallback() {
    return PasswordResetEmailCallback.getOrCreate(this);
  }


}
