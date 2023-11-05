package net.bytle.tower.eraldy.api;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.openapi.RouterBuilder;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.api.implementer.callback.ListRegistrationEmailCallback;
import net.bytle.tower.eraldy.api.implementer.callback.PasswordResetEmailCallback;
import net.bytle.tower.eraldy.api.implementer.callback.UserLoginEmailCallback;
import net.bytle.tower.eraldy.api.implementer.callback.UserRegisterEmailCallback;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiVertxSupport;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.objectProvider.*;
import net.bytle.tower.util.Guid;
import net.bytle.tower.util.OAuthExternal;
import net.bytle.tower.util.OAuthQueryProperty;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.*;

import static net.bytle.tower.util.Guid.*;

/**
 * The public api
 */
public class EraldyApiApp extends TowerApp {


  private final UserProvider userProvider;
  private final RealmProvider realmProvider;
  private final ListProvider listProvider;

  private final HashId hashIds;
  private final OrganizationProvider organizationProvider;
  private final ListRegistrationProvider listRegistrationProvider;
  private final ServiceProvider serviceProvider;
  private final OrganizationUserProvider organizationUserProvider;

  public EraldyApiApp(TowerApexDomain topLevelDomain) {
    super(topLevelDomain);
    this.realmProvider = new RealmProvider(this);
    this.userProvider = new UserProvider(this);
    this.listProvider = new ListProvider(this);
    this.organizationProvider = new OrganizationProvider(this);
    this.listRegistrationProvider = new ListRegistrationProvider(this);
    this.serviceProvider = new ServiceProvider(this);
    this.organizationUserProvider = new OrganizationUserProvider(this);
    this.hashIds = this.getApexDomain().getHttpServer().getServer().getHashId();
  }


  public static EraldyApiApp create(TowerApexDomain topLevelDomain) {

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

  public RealmProvider getRealmProvider() {
    return this.realmProvider;
  }

  public UserProvider getUserProvider() {
    return userProvider;
  }

  public ListProvider getListProvider() {
    return this.listProvider;
  }

  public AppProvider getAppProvider() {
    return new AppProvider(this);
  }


  public Guid createGuidFromHashWithOneRealmIdAndOneObjectId(String guidPrefix, String guid) throws CastException {
    return new builder(this.hashIds, guidPrefix)
      .setCipherText(guid, REALM_ID_OBJECT_ID_TYPE)
      .build();
  }

  public Guid createGuidFromRealmOrOrganizationId(String shortPrefix, String guid) throws CastException {
    return new builder(this.hashIds, shortPrefix)
      .setCipherText(guid, ONE_ID_TYPE)
      .build();
  }

  public Guid createGuidFromRealmAndObjectId(String shortPrefix, Realm realm, Long id) {

    return new builder(this.hashIds, shortPrefix)
      .setRealm(realm)
      .setId(id)
      .build();
  }

  public Guid createGuidFromObjectId(String prefix, Long id) {
    return Guid.builder(this.hashIds, prefix)
      .setOrganizationOrRealmId(id)
      .build();

  }

  public Guid createObjectFromRealmIdAndTwoObjectId(String shortPrefix, String guid) throws CastException {
    return Guid.builder(this.hashIds, shortPrefix)
      .setCipherText(guid, REALM_ID_TWO_OBJECT_ID_TYPE)
      .build();
  }

  public Guid createGuidStringFromRealmAndTwoObjectId(String shortPrefix, Realm realm, Long id1, Long id2) {

    return createGuidStringFromRealmAndTwoObjectId(shortPrefix, realm.getLocalId(), id1, id2);

  }

  public Guid createGuidStringFromRealmAndTwoObjectId(String shortPrefix, Long realmId, Long id1, Long id2) {

    return Guid.builder(this.hashIds, shortPrefix)
      .setOrganizationOrRealmId(realmId)
      .setFirstId(id1)
      .setSecondId(id2)
      .build();

  }


  public OrganizationProvider getOrganizationProvider() {
    return this.organizationProvider;
  }

  public Guid createGuidFromHashWithOneId(String shortPrefix, String guid) throws CastException {
    return Guid.builder(this.hashIds, shortPrefix)
      .setCipherText(guid, ONE_ID_TYPE)
      .build();
  }

  public ListRegistrationProvider getListRegistrationProvider() {

    return this.listRegistrationProvider;
  }

  public ServiceProvider getServiceProvider() {
    return this.serviceProvider;
  }

  public OrganizationUserProvider getOrganizationUserProvider() {
      return this.organizationUserProvider;
  }
}
