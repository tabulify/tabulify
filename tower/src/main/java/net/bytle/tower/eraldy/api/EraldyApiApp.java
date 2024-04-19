package net.bytle.tower.eraldy.api;

import io.vertx.core.Future;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.fs.Fs;
import net.bytle.java.JavaEnvs;
import net.bytle.tower.AuthClient;
import net.bytle.tower.EraldyModel;
import net.bytle.tower.eraldy.api.implementer.flow.*;
import net.bytle.tower.eraldy.auth.AuthClientHandler;
import net.bytle.tower.eraldy.auth.RealmSessionHandler;
import net.bytle.tower.eraldy.graphql.EraldyGraphQL;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.module.list.db.ListProvider;
import net.bytle.tower.eraldy.module.list.db.ListUserProvider;
import net.bytle.tower.eraldy.module.mailing.db.mailing.MailingProvider;
import net.bytle.tower.eraldy.module.mailing.db.mailingitem.MailingItemProvider;
import net.bytle.tower.eraldy.module.mailing.db.mailingjob.MailingJobProvider;
import net.bytle.tower.eraldy.module.mailing.flow.MailingFlow;
import net.bytle.tower.eraldy.module.user.jackson.JacksonEmailAddressDeserializer;
import net.bytle.tower.eraldy.module.user.jackson.JacksonEmailAddressSerializer;
import net.bytle.tower.eraldy.objectProvider.*;
import net.bytle.tower.eraldy.schedule.SqlAnalytics;
import net.bytle.tower.util.Env;
import net.bytle.tower.util.EraldySubRealmModel;
import net.bytle.tower.util.Guid;
import net.bytle.type.EmailAddress;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.*;
import net.bytle.vertx.auth.AuthNContextManager;
import net.bytle.vertx.auth.AuthQueryProperty;
import net.bytle.vertx.auth.OAuthExternalCodeFlow;
import net.bytle.vertx.db.JdbcClient;
import net.bytle.vertx.db.JdbcSchema;
import net.bytle.vertx.graphql.GraphQLService;
import net.bytle.vertx.resilience.EmailAddressValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;

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
  private final MailingFlow mailingFlow;

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

  private final AuthNContextManager authNContextManager;
  private final OrganizationRoleProvider organizationRoleProvider;

  private final RealmSequenceProvider realmSequenceProvider;
  private final MailingProvider mailingProvider;
  private final FileProvider fileProvider;
  private final MailingJobProvider mailingJobProvider;
  private final MailingItemProvider mailingRowProvider;



  public EraldyApiApp(HttpServer httpServer) throws ConfigIllegalException {
    super(httpServer, EraldyDomain.getOrCreate(httpServer));


    ConfigAccessor configAccessor = httpServer.getServer().getConfigAccessor();

    /**
     * Client Session and auth first as they should be added first on the router
     */
    String realmHandleContextAndSessionKey = "ey-realm-handle";
    String realmGuidContextAndSessionKey = "ey-realm-guid";
    /**
     * Auth client
     * Determine the auth client (and therefore the realm)
     * and put it on the routingContext
     * As a user can log in to only on realm, the session cookie has the name
     * of the realm in its name
     * This handler should then be mounted before the session handler
     */
    this.authClientIdHandler = AuthClientHandler.config(this)
      .setRealmGuidContextKey(realmGuidContextAndSessionKey)
      .setRealmHandleContextKey(realmHandleContextAndSessionKey)
      .build();

    /**
     * Session Handlers needs the client id
     * We add/mount the session after the clientId handler
     */
    int cookieMaxAgeOneWeekInSec = 60 * 60 * 24 * 7;
    int idleSessionTimeoutMs = cookieMaxAgeOneWeekInSec * 1000; // Reconnect once every day
    RealmSessionHandler
      .createForApp(this)
      .setSessionTimeout(idleSessionTimeoutMs)
      .setRealmHandleContextAndSessionKey(realmHandleContextAndSessionKey)
      .setRealmGuidContextAndSessionKey(realmGuidContextAndSessionKey)
      .setCookieMaxAge(cookieMaxAgeOneWeekInSec)
      .setFailIfRealmNotFound(false);


    // data directory
    Path runtime = Paths.get("data/runtime");
    if (JavaEnvs.IS_DEV) {
      // put it in the build
      runtime = Paths.get("build/" + this.getAppHandle().toLowerCase() + "/data/runtime");
    }
    this.runtimeDataDirectory = configAccessor.getPath(RUNTIME_DATA_DIR_CONF, runtime);
    Fs.createDirectoryIfNotExists(this.runtimeDataDirectory);

    /**
     * Schema
     */
    JdbcClient postgresClient = httpServer.getServer().getPostgresClient();
    JdbcSchema realmSchema = JdbcSchema.builder(postgresClient, "realms").build();
    JdbcSchema jobsSchema = JdbcSchema.builder(postgresClient, "jobs")
      .setJavaPackageForClassGeneration("net.bytle.jobs")
      .build();

    /**
     * Jackson common type
     * Email Address
     * (Must be before the provider below as they make use of it)
     */
    httpServer.getServer().getJacksonMapperManager()
      .addDeserializer(EmailAddress.class, new JacksonEmailAddressDeserializer())
      .addSerializer(EmailAddress.class, new JacksonEmailAddressSerializer());

    /**
     * DataBase Provider/Manager
     */
    this.userProvider = new UserProvider(this, realmSchema);
    this.organizationUserProvider = new OrganizationUserProvider(this);
    this.realmProvider = new RealmProvider(this, realmSchema);
    this.listProvider = new ListProvider(this, realmSchema);
    this.listImportFlow = new ListImportFlow(this);
    this.organizationProvider = new OrganizationProvider(this);
    this.authProvider = new AuthProvider(this);
    this.listUserProvider = new ListUserProvider(this);
    this.serviceProvider = new ServiceProvider(this);
    this.organizationRoleProvider = new OrganizationRoleProvider(this);
    this.hashIds = this.getHttpServer().getServer().getHashId();
    this.authClientProvider = new AuthClientProvider(this);
    this.realmSequenceProvider = new RealmSequenceProvider();
    this.mailingProvider = new MailingProvider(this, realmSchema);
    this.mailingJobProvider = new MailingJobProvider(this, jobsSchema);
    this.mailingRowProvider = new MailingItemProvider(this, jobsSchema);
    this.fileProvider = new FileProvider(this);

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
    this.mailingFlow = new MailingFlow(this);

    /**
     * OAuth Service
     */
    AuthNContextManager oAuthContextManager = AuthNContextManager.builder()
      .addContextHandler(this.userRegistrationFlow.handleOAuthAuthentication())
      .addContextHandler(this.userListRegistrationFlow.handleStepOAuthAuthentication())
      .setRealmGuidSessionKey(realmGuidContextAndSessionKey)
      .build();
    this.oauthExternalFlow = new OAuthExternalCodeFlow(this, "/auth/oauth", oAuthContextManager);


    /**
     * OpenApi
     */
    new OpenApiService(new EraldyOpenApi(this));

    /**
     * GraphQL
     */
    new GraphQLService(new EraldyGraphQL(this));

    /**
     * The authN manager used by all flows to authenticate a user
     */
    this.authNContextManager = AuthNContextManager.builder().setRealmGuidSessionKey(realmGuidContextAndSessionKey).build();

    /**
     * Utility
     */
    new SqlAnalytics(this);
    this.emailAddressValidator = new EmailAddressValidator(this);


  }


  public static EraldyApiApp createForHttpServer(HttpServer httpServer) throws ConfigIllegalException {

    return new EraldyApiApp(httpServer);

  }


  @Override
  public String getAppHandle() {
    return "Api";
  }


  @Override
  protected String getPublicSubdomainName() {
    return "api";
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

  public ListUserProvider getListUserProvider() {

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



    TowerApexDomain apexDomain = this.getApexDomain();
    LOGGER.info("Allow CORS on the domain (" + apexDomain + ")");
    // Allow Browser cross-origin request in the domain
    BrowserCorsUtil.allowCorsForApexDomain(this.getHttpServer().getRouter(), this);


    /**
     * The Eraldy base realm and base apps
     */
    return eraldyModel.mount()
      .recover(err -> Future.failedFuture(new InternalException("Eraldy model mount has failed", err)))
      .compose(v -> {
        /**
         * Dev only
         */
        Future<Void> datacadamiaModel = Future.succeededFuture();
        if (Env.IS_DEV) {
          // Add a sub-realm for test/purpose only
          datacadamiaModel = eraldySubRealmModel.insertModelInDatabase();
        }
        return datacadamiaModel;
      })
      .recover(err -> Future.failedFuture(new InternalException("Datacadamia model mount has failed", err)))
      .compose(v -> super.mount());
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

  public AuthNContextManager getAuthNContextManager() {
    return this.authNContextManager;
  }

  public OrganizationRoleProvider getOrganizationRoleProvider() {
    return this.organizationRoleProvider;
  }


  public RealmSequenceProvider getRealmSequenceProvider() {
    return this.realmSequenceProvider;
  }


  public MailingProvider getMailingProvider() {
    return this.mailingProvider;
  }

  @SuppressWarnings("unused")
  public FileProvider getFileProvider() {
    return this.fileProvider;
  }

  public TowerSmtpClientService getEmailSmtpClientService() {
    return this.getHttpServer().getServer().getSmtpClient();
  }

  public MailingFlow getMailingFlow() {
    return this.mailingFlow;
  }

  public MailingJobProvider getMailingJobProvider() {
    return this.mailingJobProvider;
  }

  public MailingItemProvider getMailingItemProvider() {
    return this.mailingRowProvider;
  }
}
