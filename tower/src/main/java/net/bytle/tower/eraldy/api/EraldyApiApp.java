package net.bytle.tower.eraldy.api;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.Operation;
import io.vertx.ext.web.openapi.RouterBuilder;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.fs.Fs;
import net.bytle.java.JavaEnvs;
import net.bytle.tower.ApiClient;
import net.bytle.tower.EraldyModel;
import net.bytle.tower.eraldy.api.implementer.flow.*;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiVertxSupport;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.objectProvider.*;
import net.bytle.tower.eraldy.schedule.SqlAnalytics;
import net.bytle.tower.util.Env;
import net.bytle.tower.util.EraldySubRealmModel;
import net.bytle.tower.util.Guid;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.*;
import net.bytle.vertx.auth.ApiSessionAuthenticationHandler;
import net.bytle.vertx.auth.AuthContext;
import net.bytle.vertx.auth.AuthQueryProperty;
import net.bytle.vertx.auth.OAuthExternalCodeFlow;
import net.bytle.vertx.resilience.EmailAddressValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
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
  /**
   * The URI of the member app
   */
  private static final String MEMBER_APP_URI_CONF = "member.app.uri";
  private final UserProvider userProvider;
  private final RealmProvider realmProvider;
  private final ListProvider listProvider;

  private final HashId hashIds;
  private final OrganizationProvider organizationProvider;
  private final ListUserProvider listUserProvider;
  private final ServiceProvider serviceProvider;
  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final OrganizationUserProvider organizationUserProvider;
  private final URI memberAppUri;
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
  private final ApiClientProvider apiClientProvider;

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
    this.apiClientProvider = new ApiClientProvider(this);

    /**
     * Model
     */
    this.eraldyModel = new EraldyModel(this);
    this.eraldySubRealmModel = EraldySubRealmModel.getOrCreate(this);

    /**
     * For redirect
     */
    String memberUri = configAccessor.getString(MEMBER_APP_URI_CONF, "https://member." + apexDomain.getApexNameWithPort());
    try {
      this.memberAppUri = URI.create(memberUri);
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
  public UriEnhanced getMemberLoginUri(UriEnhanced redirectUri, ApiClient apiClient) {

    return this.getMemberAppUri()
      .setPath("/login")
      .addQueryProperty(AuthQueryProperty.REDIRECT_URI, redirectUri.toString())
      .addQueryProperty(AuthQueryProperty.CLIENT_ID, apiClient.getGuid());
  }

  public UriEnhanced getMemberAppUri() {

    return UriEnhanced.createFromUri(this.memberAppUri);
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

  public boolean isEraldyRealm(Realm realm) {
    return getEraldyRealm().getLocalId().equals(realm.getLocalId());
  }

  public Realm getEraldyRealm() {
    return this.eraldyModel.getRealm();
  }

  public boolean isEraldyRealm(Long localId) {
    return this.eraldyModel.isEraldyRealm(localId);
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


    Future<Void> eraldyOrg = eraldyModel.insertModelInDatabase();
    Future<Void> parentMount = super.mount();

    return Future
      .all(eraldyOrg, parentMount)
      .recover(err -> Future.failedFuture(new InternalException("One of the Api App mount future has failed", err)))
      .compose(v -> {
        Future<Void> datacadamiaModel = Future.succeededFuture();
        if (Env.IS_DEV) {
          // Add a sub-realm for test/purpose only
          datacadamiaModel = eraldySubRealmModel.insertModelInDatabase();
        }
        return datacadamiaModel;
      });
  }

  public ApiClientProvider getApiClientProvider() {
     return this.apiClientProvider;
  }
}
