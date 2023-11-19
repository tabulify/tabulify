package net.bytle.tower.eraldy.api;

import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.Operation;
import io.vertx.ext.web.openapi.RouterBuilder;
import net.bytle.exception.CastException;
import net.bytle.exception.IllegalConfiguration;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.implementer.flow.EmailLoginFlow;
import net.bytle.tower.eraldy.api.implementer.flow.ListRegistrationFlow;
import net.bytle.tower.eraldy.api.implementer.flow.PasswordResetFlow;
import net.bytle.tower.eraldy.api.implementer.flow.UserRegistrationFlow;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiVertxSupport;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.objectProvider.*;
import net.bytle.tower.util.Guid;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.*;
import net.bytle.vertx.auth.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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
  private final URI memberApp;
  private final UserRegistrationFlow userRegistrationFlow;
  private final ListRegistrationFlow userListRegistrationFlow;
  private final EmailLoginFlow emailLoginFlow;
  private final OAuthExternalCodeFlow oauthExternalFlow;

  public EraldyApiApp(TowerApexDomain apexDomain) throws ConfigIllegalException {
    super(apexDomain);
    this.realmProvider = new RealmProvider(this);
    this.userProvider = new UserProvider(this);
    this.listProvider = new ListProvider(this);
    this.organizationProvider = new OrganizationProvider(this);
    this.listRegistrationProvider = new ListRegistrationProvider(this);
    this.serviceProvider = new ServiceProvider(this);
    this.organizationUserProvider = new OrganizationUserProvider(this);
    this.hashIds = this.getApexDomain().getHttpServer().getServer().getHashId();
    String memberUri = apexDomain.getHttpServer().getServer().getConfigAccessor().getString(MEMBER_APP_URI_CONF, "https://member." + apexDomain.getApexNameWithPort());
    try {
      this.memberApp = URI.create(memberUri);
      LOGGER.info("The member app URI was set to ({}) via the conf ({})", memberUri, MEMBER_APP_URI_CONF);
    } catch (Exception e) {
      throw new ConfigIllegalException("The member app value (" + memberUri + ") of the conf (" + MEMBER_APP_URI_CONF + ") is not a valid URI", e);
    }
    /**
     * Flow management
     */
    this.userRegistrationFlow = new UserRegistrationFlow(this);
    this.userListRegistrationFlow = new ListRegistrationFlow(this);
    this.emailLoginFlow = new EmailLoginFlow(this);
    List<Handler<AuthContext>> authHandlers = new ArrayList<>();
    authHandlers.add(this.userRegistrationFlow.handleOAuthAuthentication());
    authHandlers.add(this.userListRegistrationFlow.handleStepOAuthAuthentication());
    this.oauthExternalFlow = new OAuthExternalCodeFlow(this, "/auth/oauth", authHandlers);

  }


  public static EraldyApiApp create(TowerApexDomain topLevelDomain) throws ConfigIllegalException {

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
    ApiSessionAuthenticationHandler cookieAuthHandler = new ApiSessionAuthenticationHandler();
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
    this.oauthExternalFlow.step2AddProviderAndCallbacks(router);


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
  public UriEnhanced getMemberLoginUri(String redirectUri, String realmIdentifier) {

    return this.getMemberAppUri()
      .setPath("/login")
      .addQueryProperty(AuthQueryProperty.REDIRECT_URI, redirectUri)
      .addQueryProperty(AuthQueryProperty.REALM_IDENTIFIER, realmIdentifier);
  }

  public UriEnhanced getMemberAppUri() {

    return UriEnhanced.createFromUri(this.memberApp);
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
    AuthUser authUser = AuthUser.createFromClaims(user.principal().mergeIn(user.attributes()));
    return UsersUtil.toEraldyUser(authUser, this);

  }


  public OAuthExternalCodeFlow getOauthFlow() {
    return this.oauthExternalFlow;
  }

  public OrganizationUserProvider getOrganizationUserProvider() {
    return this.organizationUserProvider;
  }
}
