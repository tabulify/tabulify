package net.bytle.tower.eraldy.api;

import io.vertx.ext.auth.authorization.RoleBasedAuthorization;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.APIKeyHandler;
import io.vertx.ext.web.handler.ChainAuthHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import net.bytle.exception.CastException;
import net.bytle.exception.IllegalConfiguration;
import net.bytle.exception.IllegalStructure;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

import static net.bytle.tower.util.Guid.*;

/**
 * The public api
 */
public class EraldyApiApp extends TowerApp {


  static Logger LOGGER = LogManager.getLogger(EraldyApiApp.class);
  private static final String MEMBER_APP_URI_CONF = "member.app.uri";
  private final UserProvider userProvider;
  private final RealmProvider realmProvider;
  private final ListProvider listProvider;

  private final HashId hashIds;
  private final OrganizationProvider organizationProvider;
  private final ListRegistrationProvider listRegistrationProvider;
  private final ServiceProvider serviceProvider;
  private final OrganizationUserProvider organizationUserProvider;
  private final UriEnhanced memberApp;

  public EraldyApiApp(TowerApexDomain topLevelDomain) throws IllegalConfiguration {
    super(topLevelDomain);
    this.realmProvider = new RealmProvider(this);
    this.userProvider = new UserProvider(this);
    this.listProvider = new ListProvider(this);
    this.organizationProvider = new OrganizationProvider(this);
    this.listRegistrationProvider = new ListRegistrationProvider(this);
    this.serviceProvider = new ServiceProvider(this);
    this.organizationUserProvider = new OrganizationUserProvider(this);
    this.hashIds = this.getApexDomain().getHttpServer().getServer().getHashId();
    String memberUri = topLevelDomain.getHttpServer().getServer().getConfigAccessor().getString(MEMBER_APP_URI_CONF, "https://member." + topLevelDomain.getApexName());
    try {
      this.memberApp = UriEnhanced.createFromString(memberUri);
      LOGGER.info("The member app URI was set to ({}) via the conf ({})", memberUri, MEMBER_APP_URI_CONF);
    } catch (IllegalStructure e) {
      throw new IllegalConfiguration("The member app value (" + memberUri + ") of the conf (" + MEMBER_APP_URI_CONF + ") is not a valid URI", e);
    }
  }


  public static EraldyApiApp create(TowerApexDomain topLevelDomain) throws IllegalConfiguration {

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
  public EraldyApiApp openApiBindSecurityScheme(RouterBuilder builder, ConfigAccessor configAccessor) throws IllegalConfiguration {

    /**
     * Configuring `AuthenticationHandler`s defined in the OpenAPI document
     * https://vertx.io/docs/vertx-web-openapi/java/#_configuring_authenticationhandlers_defined_in_the_openapi_document
     */
    EraldyApiApp apiApp = this;
    APIKeyHandler apiKeyAuthHandler = apiApp.getApexDomain().getHttpServer().getApiKeyAuthHandler();

    ChainAuthHandler chain = ChainAuthHandler.all()
      .add(apiKeyAuthHandler);
      //.add(apiApp::authorizationCheckHandler);
    builder
      .securityHandler(OpenApiSecurityNames.APIKEY_AUTH_SECURITY_SCHEME)
      .bindBlocking(config -> chain);

//    builder
//      .securityHandler(OpenApiSecurityNames.BEARER_AUTH_SECURITY_SCHEME)
//      .bindBlocking(config -> this.getApexDomain().getHttpServer().getBearerAuthenticationHandler());

    APIKeyHandler cookieAuthHandler = this.getApexDomain().getHttpServer().getCookieAuthHandler();
    builder
      .securityHandler(OpenApiSecurityNames.COOKIE_SECURITY_SCHEME)
      .bindBlocking(config -> ChainAuthHandler
        .all()
        .add(cookieAuthHandler)
        //.add(apiApp::authorizationCheckHandler)
      );

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
   * @return the login uri used for redirection in case of non-authentication
   * For an API, it's a no-sense but yeah
   */
  public UriEnhanced getLoginUriForEraldyRealm(String redirectUri) {

    return this.memberApp.setPath("/login")
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

  public void authorizationCheckHandler(RoutingContext routingContext) {

    Set<String> scopes = this.getOpenApi().getScopes(routingContext);

    for (String scope : scopes) {
      if (!RoleBasedAuthorization.create(scope).match(routingContext.user())) {
        routingContext.fail(HttpStatus.NOT_AUTHORIZED.httpStatusCode());
      }
    }

    routingContext.next();
  }
}
