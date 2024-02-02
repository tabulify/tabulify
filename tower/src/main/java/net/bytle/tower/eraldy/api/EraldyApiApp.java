package net.bytle.tower.eraldy.api;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.Operation;
import io.vertx.ext.web.openapi.RouterBuilder;
import net.bytle.exception.CastException;
import net.bytle.exception.DbMigrationException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NullValueException;
import net.bytle.fs.Fs;
import net.bytle.java.JavaEnvs;
import net.bytle.tower.AuthClient;
import net.bytle.tower.EraldyModel;
import net.bytle.tower.eraldy.api.implementer.flow.*;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiVertxSupport;
import net.bytle.tower.eraldy.auth.ApiKeyAndSessionUserAuthenticationHandler;
import net.bytle.tower.eraldy.auth.AuthClientHandler;
import net.bytle.tower.eraldy.auth.EraldySessionHandler;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.objectProvider.*;
import net.bytle.tower.eraldy.schedule.SqlAnalytics;
import net.bytle.tower.util.Env;
import net.bytle.tower.util.EraldySubRealmModel;
import net.bytle.tower.util.Guid;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.*;
import net.bytle.vertx.auth.ApiKeyAuthenticationProvider;
import net.bytle.vertx.auth.AuthContext;
import net.bytle.vertx.auth.AuthQueryProperty;
import net.bytle.vertx.auth.OAuthExternalCodeFlow;
import net.bytle.vertx.resilience.EmailAddressValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static net.bytle.tower.util.Guid.*;

/**
 * The api application
 */
public class EraldyApiApp extends TowerApp {


  private static final String RUNTIME_DATA_DIR_CONF = "data.runtime.dir.path";

  static Logger LOGGER = LogManager.getLogger(EraldyApiApp.class);
  private final UserProvider userProvider;
  private final RealmProvider realmProvider;
  private final ListProvider listProvider;

  private final HashId hashIds;
  private final OrganizationProvider organizationProvider;
  private final ListUserProvider listUserProvider;
  private final ServiceProvider serviceProvider;
  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final OrganizationUserProvider organizationUserProvider;
  private final UserRegistrationFlow userRegistrationFlow;
  private final ListRegistrationFlow userListRegistrationFlow;
  private final EmailLoginFlow emailLoginFlow;
  private final OAuthExternalCodeFlow oauthExternalFlow;
  private final AuthProvider authProvider;
  private final EmailAddressValidator emailAddressValidator;
  private final ListImportFlow listImportFlow;
  /**
   * Data that are used during runtime
   * (example: list import result)
   * We keep them in a transient way
   * until there is no space on the VPS ...
   */
  private final Path runtimeDataDirectory;
  private final PasswordLoginFlow passwordLoginFlow;
  private final EraldyModel eraldyModel;
  private final EraldySubRealmModel eraldySubRealmModel;
  private final AuthClientProvider authClientProvider;
  private final AuthClientHandler authClientIdHandler;
  private final EraldySessionHandler sessionHandler;
  private final ApiKeyAuthenticationProvider apiKeyUserProvider;


  public EraldyApiApp(TowerApexDomain apexDomain) throws ConfigIllegalException {
    super(apexDomain);
    ConfigAccessor configAccessor = apexDomain.getHttpServer().getServer().getConfigAccessor();

    // data directory
    Path runtime = Paths.get("data/runtime");
    if (JavaEnvs.IS_DEV) {
      // put it in the build
      runtime = Paths.get("build/" + this.getAppName().toLowerCase() + "/data/runtime");
    }
    this.runtimeDataDirectory = configAccessor.getPath(RUNTIME_DATA_DIR_CONF, runtime);
    Fs.createDirectoryIfNotExists(this.runtimeDataDirectory);

    /**
     * DataBase Provider/Manager
     */
    this.realmProvider = new RealmProvider(this);
    this.userProvider = new UserProvider(this);
    this.listProvider = new ListProvider(this);
    this.listImportFlow = new ListImportFlow(this);
    this.organizationProvider = new OrganizationProvider(this);
    this.authProvider = new AuthProvider(this);
    this.listUserProvider = new ListUserProvider(this);
    this.serviceProvider = new ServiceProvider(this);
    this.organizationUserProvider = new OrganizationUserProvider(this);
    this.hashIds = this.getApexDomain().getHttpServer().getServer().getHashId();
    this.authClientProvider = new AuthClientProvider(this);

    /**
     * Model and app
     */
    this.eraldyModel = new EraldyModel(this);
    this.eraldySubRealmModel = EraldySubRealmModel.getOrCreate(this);

    /**
     * Flow management
     */
    this.userRegistrationFlow = new UserRegistrationFlow(this);
    this.userListRegistrationFlow = new ListRegistrationFlow(this);
    this.emailLoginFlow = new EmailLoginFlow(this);
    this.passwordLoginFlow = new PasswordLoginFlow(this);
    List<Handler<AuthContext>> authHandlers = new ArrayList<>();
    authHandlers.add(this.userRegistrationFlow.handleOAuthAuthentication());
    authHandlers.add(this.userListRegistrationFlow.handleStepOAuthAuthentication());
    this.oauthExternalFlow = new OAuthExternalCodeFlow(this, "/auth/oauth", authHandlers);

    /**
     * Utility
     */
    new SqlAnalytics(this);
    this.emailAddressValidator = new EmailAddressValidator(this);

    /**
     * Handlers
     */
    /**
     * Determine the auth client (and therefore the realm)
     * and put it on the routingContext
     * As a user can log in to only on realm, the session cookie has the name
     * of the realm in its name
     * This handler should then be mounted before the session handler
     */
    String realmHandleContextKey = "ey-realm-handle";
    this.authClientIdHandler = AuthClientHandler.config(this)
      .setRealmHandleContextKey(realmHandleContextKey)
      .build();
    /**
     * Reconnect once every
     */
    int cookieMaxAgeOneWeekInSec = 60 * 60 * 24 * 7;
    /**
     * Delete the session if not accessed within this timeout
     */
    int idleSessionTimeoutMs = cookieMaxAgeOneWeekInSec * 1000;
    this.sessionHandler = EraldySessionHandler
      .createWithDomain(this.getApexDomain())
      .setSessionTimeout(idleSessionTimeoutMs)
      .setRealmHandleContextKey(realmHandleContextKey)
      .setCookieMaxAge(cookieMaxAgeOneWeekInSec);

    /**
     * OpenApi Auth Handler
     */
    /**
     * Add the API Key authentication handler
     * on the router to fill the user in the context
     * as Api Key is supported
     */
    try {
      this.apiKeyUserProvider = this.getApexDomain().getHttpServer().getServer().getApiKeyAuthProvider();
    } catch (NullValueException e) {
      throw new ConfigIllegalException("Api Key should be enabled", e);
    }


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
  public EraldyApiApp openApiBindSecurityScheme(RouterBuilder routerBuilder, ConfigAccessor configAccessor) {

    /**
     * We trick the open api security scheme apiKey define in the openapi file
     * to support a cookie authentication by realm
     * This scheme below is implemented by
     * the {@link ApiAuthenticationHandler} that just check if the user is on the vertx context.
     * <p>
     * We to add the needed Authentication handler to fill the user
     * {@link #mountSessionHandlers()}
     */
    routerBuilder
      .securityHandler(OpenApiSecurityNames.APIKEY_AUTH_SECURITY_SCHEME)
      .bindBlocking(jsonObject -> {
        String type = jsonObject.getString("type");
        if (!type.equals("apiKey")) {
          throw new InternalException("The security scheme type should be apiKey, not " + type);
        }
        String in = jsonObject.getString("in");
        if (!in.equals("header")) {
          throw new InternalException("The security scheme in should be a header, not " + in);
        }
        String headerName = jsonObject.getString("name");
        return new ApiKeyAndSessionUserAuthenticationHandler(this,headerName, apiKeyUserProvider);
      });

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
      .getStep2Callback()
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
  public UriEnhanced getMemberLoginUri(UriEnhanced redirectUri, AuthClient authClient) {

    return this.getEraldyModel().getMemberAppUri()
      .setPath("/login")
      .addQueryProperty(AuthQueryProperty.REDIRECT_URI, redirectUri.toString())
      .addQueryProperty(AuthQueryProperty.CLIENT_ID, authClient.getGuid());
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

  public Guid createGuidFromHashWithOneRealmIdAndTwoObjectId(String guidPrefix, String guid) throws CastException {
    return new builder(this.hashIds, guidPrefix)
      .setCipherText(guid, REALM_ID_TWO_OBJECT_ID_TYPE)
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
      .setFirstObjectId(id)
      .build();
  }

  public Guid createGuidFromRealmAndObjectId(String shortPrefix, Long realmId, Long id) {

    return new builder(this.hashIds, shortPrefix)
      .setOrganizationOrRealmId(realmId)
      .setFirstObjectId(id)
      .build();
  }

  public Guid createGuidFromObjectId(String prefix, Long id) {
    return Guid.builder(this.hashIds, prefix)
      .setOrganizationOrRealmId(id)
      .build();

  }

  public Guid createGuidStringFromRealmAndTwoObjectId(String shortPrefix, Long realmId, Long id1, Long id2) {

    return Guid.builder(this.hashIds, shortPrefix)
      .setOrganizationOrRealmId(realmId)
      .setFirstObjectId(id1)
      .setSecondObjectId(id2)
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

  public ListUserProvider getListRegistrationProvider() {

    return this.listUserProvider;
  }

  public ServiceProvider getServiceProvider() {
    return this.serviceProvider;
  }


  public PasswordResetFlow getPasswordResetFlow() {
    return new PasswordResetFlow(this);
  }


  public OAuthExternalCodeFlow getOauthFlow() {
    return this.oauthExternalFlow;
  }

  public OrganizationUserProvider getOrganizationUserProvider() {
    return this.organizationUserProvider;
  }

  public AuthProvider getAuthProvider() {
    return this.authProvider;
  }

  public EmailAddressValidator getEmailAddressValidator() {
    return this.emailAddressValidator;
  }

  /**
   * Do we output debug information
   * to the user?
   */
  public boolean addDebugInfo() {
    return JavaEnvs.IS_DEV;
  }

  public ListImportFlow getListImportFlow() {
    return this.listImportFlow;
  }

  public Path getRuntimeDataDirectory() {

    return this.runtimeDataDirectory;
  }

  public PasswordLoginFlow getPasswordLoginFlow() {
    return this.passwordLoginFlow;
  }

  @Override
  public Future<Void> mount() {

    /**
     * Session Handlers needs the client id
     * (because we support api key, the api key authentication handler of the open api spec
     * should have run - ie {@link #openApiBindSecurityScheme(RouterBuilder, ConfigAccessor)}
     * We mount the session after then
     */
    LOGGER.info("Add Auth Session Cookie");
    Router router = this.getApexDomain().getHttpServer().getRouter();
    router.route().handler(this.authClientIdHandler);
    router.route().handler(this.sessionHandler);

    TowerApexDomain apexDomain = this.getApexDomain();
    LOGGER.info("Allow CORS on the domain (" + apexDomain + ")");
    // Allow Browser cross-origin request in the domain
    BrowserCorsUtil.allowCorsForApexDomain(router, apexDomain);

    LOGGER.info("EraldyApp Db Migration");
    JdbcConnectionInfo postgresDatabaseConnectionInfo = apexDomain.getHttpServer().getServer().getPostgresDatabaseConnectionInfo();
    JdbcSchemaManager jdbcSchemaManager = JdbcSchemaManager.create(postgresDatabaseConnectionInfo);
    // Realms
    String schema = JdbcSchemaManager.getSchemaFromHandle("realms");
    JdbcSchema realmSchema = JdbcSchema.builder()
      .setLocation("classpath:db/cs-realms")
      .setSchema(schema)
      .build();
    try {
      jdbcSchemaManager.migrate(realmSchema);
    } catch (DbMigrationException e) {
      return Future.failedFuture(new InternalException("The database migration failed", e));
    }

    Future<Void> eraldyOrg = eraldyModel.insertModelInDatabase();
    Future<Void> parentMount = super.mount();

    return Future
      .all(eraldyOrg, parentMount)
      .recover(err -> Future.failedFuture(new InternalException("One of the Api App mount future has failed", err)))
      .compose(v -> {

        /**
         * Model
         */
        Future<Void> datacadamiaModel = Future.succeededFuture();
        if (Env.IS_DEV) {
          // Add a sub-realm for test/purpose only
          datacadamiaModel = eraldySubRealmModel.insertModelInDatabase();
        }
        return datacadamiaModel;
      });
  }

  public AuthClientProvider getAuthClientProvider() {
    return this.authClientProvider;
  }

  public EraldyModel getEraldyModel() {
    return this.eraldyModel;
  }

  public AuthClientHandler getAuthClientIdHandler() {
    return this.authClientIdHandler;
  }

}
