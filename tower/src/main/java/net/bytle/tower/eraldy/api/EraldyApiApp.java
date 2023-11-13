package net.bytle.tower.eraldy.api;

import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.APIKeyHandler;
import io.vertx.ext.web.openapi.Operation;
import io.vertx.ext.web.openapi.RouterBuilder;
import net.bytle.exception.CastException;
import net.bytle.exception.IllegalConfiguration;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.implementer.flow.EmailLoginFlow;
import net.bytle.tower.eraldy.api.implementer.flow.ListRegistrationFlow;
import net.bytle.tower.eraldy.api.implementer.flow.PasswordResetFlow;
import net.bytle.tower.eraldy.api.implementer.flow.UserRegistrationFlow;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiVertxSupport;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.objectProvider.*;
import net.bytle.tower.util.Guid;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.*;
import net.bytle.vertx.auth.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final OrganizationUserProvider organizationUserProvider;
  private final UriEnhanced memberApp;
  private final UserRegistrationFlow userRegistrationFlow;
  private final ListRegistrationFlow userListRegistrationFlow;
  private final OAuthExternal oauthExternal;
  private final EmailLoginFlow emailLoginFlow;

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
    String memberUri = topLevelDomain.getHttpServer().getServer().getConfigAccessor().getString(MEMBER_APP_URI_CONF, "https://member." + topLevelDomain.getApexNameWithPort());
    try {
      this.memberApp = UriEnhanced.createFromString(memberUri);
      LOGGER.info("The member app URI was set to ({}) via the conf ({})", memberUri, MEMBER_APP_URI_CONF);
    } catch (IllegalStructure e) {
      throw new IllegalConfiguration("The member app value (" + memberUri + ") of the conf (" + MEMBER_APP_URI_CONF + ") is not a valid URI", e);
    }
    this.userRegistrationFlow = new UserRegistrationFlow(this);
    this.userListRegistrationFlow = new ListRegistrationFlow(this);
    this.emailLoginFlow = new EmailLoginFlow(this);
    this.oauthExternal = new OAuthExternal(this, "/auth/oauth");

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
  public EraldyApiApp openApiBindSecurityScheme(RouterBuilder routerBuilder, ConfigAccessor configAccessor) throws IllegalConfiguration {

    /**
     * Utility variables
     */
    HttpServer httpServer = this.getApexDomain().getHttpServer();


    /**
     * Configuring the handler for api key security scheme
     * <p>
     * Note: Configuring `AuthenticationHandler`s defined in the OpenAPI document
     * https://vertx.io/docs/vertx-web-openapi/java/#_configuring_authenticationhandlers_defined_in_the_openapi_document
     */
    routerBuilder
      .securityHandler(OpenApiSecurityNames.APIKEY_AUTH_SECURITY_SCHEME)
      .bindBlocking(config -> httpServer.getApiKeyAuthHandler()
      );

    /**
     * Configuring the handler for cookie security scheme
     */
    APIKeyHandler cookieAuthHandler = httpServer.getCookieAuthHandler();
    routerBuilder
      .securityHandler(OpenApiSecurityNames.COOKIE_SECURITY_SCHEME)
      .bindBlocking(config -> cookieAuthHandler);

    Handler<RoutingContext> authorizationHandler = this.getOpenApi().authorizationCheckHandler();
    for (Operation operation : routerBuilder.operations()) {
      routerBuilder.operation(operation.getOperationId())
        .handler(authorizationHandler);
    }
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
    this.oauthExternal
      .addExternal(OAuthExternalGithub.GITHUB_TENANT, router)
      .addExternal(OAuthExternalGoogle.GOOGLE_TENANT, router);

    /**
     * Add the registration validation callback
     */
    getUserRegistrationFlow()
      .getCallback()
      .addCallback(router);

    /**
     * Add the email login validation callback
     */
    getUserEmailLoginFlow()
      .getCallback()
      .addCallback(router);

    /**
     * Add the password reset callback
     */
    getPasswordResetFlow()
      .getPasswordResetCallback()
      .addCallback(router);

    /**
     * Add the user list registration callback
     */
    getUserListRegistrationFlow()
      .getCallback()
      .addCallback(router);

    return this;
  }

  public EmailLoginFlow getUserEmailLoginFlow() {
    return this.emailLoginFlow;
  }

  public ListRegistrationFlow getUserListRegistrationFlow() {
    return this.userListRegistrationFlow;
  }

  /**
   * @return the registration validation manager
   */
  public UserRegistrationFlow getUserRegistrationFlow() {
    return this.userRegistrationFlow;
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
  public UriEnhanced getLoginUri(String redirectUri, String realmIdentifier) {

    return this.memberApp.setPath("/login")
      .addQueryProperty(AuthQueryProperty.REDIRECT_URI, redirectUri)
      .addQueryProperty(AuthQueryProperty.REALM_IDENTIFIER, realmIdentifier);
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


  public PasswordResetFlow getPasswordResetFlow() {
    return new PasswordResetFlow(this);
  }


  /**
   * @param ctx - the context
   * @return the authenticated user (only auth information ie id, guid, email, ...)
   * @throws NotFoundException - not authenticated
   */
  public User getAuthSignedInUser(RoutingContext ctx) throws NotFoundException {
    io.vertx.ext.auth.User user = ctx.user();
    if (user == null) {
      throw new NotFoundException();
    }
    AuthUser authUser = user.principal().mapTo(AuthUser.class);
    Realm realm = new Realm();
    realm.setGuid(authUser.getAudience());
    try {
      Guid realmGuid = this.getRealmProvider().getGuidFromHash(authUser.getAudience());
      realm.setLocalId(realmGuid.getRealmOrOrganizationId());
    } catch (CastException e) {
      throw new RuntimeException(e);
    }
    User userEraldy = new User();
    userEraldy.setRealm(realm);
    userEraldy.setName(authUser.getSubjectGivenName());
    userEraldy.setEmail(authUser.getSubjectEmail());
    try {
      String subject = authUser.getSubject();
      userEraldy.setGuid(subject);
      Guid guid = this.getUserProvider().getGuid(subject);
      userEraldy.setLocalId(guid.validateRealmAndGetFirstObjectId(realm.getLocalId()));
    } catch (CastException e) {
      throw new RuntimeException(e);
    }
    return userEraldy;

    /**
     * For OpenID Connect/OAuth2 Access Tokens,
     * there is a rootClaim
     */
//    String rootClaim = user.attributes().getString("rootClaim");
//    if (rootClaim != null && rootClaim.equals("accessToken")) {
//      // JWT
//      String userGuid = user.principal().getString("sub");
//      if (userGuid == null) {
//        return Future.failedFuture(ValidationException.create("The sub is empty", "sub", null));
//      }
//      if(clazz.equals(OrganizationUser.class)) {
//        //noinspection unchecked
//        return (Future<T>) this
//          .getOrganizationUserProvider()
//          .getOrganizationUserByGuid(userGuid);
//      }
//    }


  }

  public OAuthExternal getOAuthExternal() {
    return this.oauthExternal;
  }

}
