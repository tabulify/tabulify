package net.bytle.tower;

import io.vertx.core.Future;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.ListObject;
import net.bytle.tower.eraldy.model.openapi.OrgaUser;
import net.bytle.tower.eraldy.module.app.inputs.AppInputProps;
import net.bytle.tower.eraldy.module.app.model.AppGuid;
import net.bytle.tower.eraldy.module.auth.db.AuthClientProvider;
import net.bytle.tower.eraldy.module.auth.model.CliGuid;
import net.bytle.tower.eraldy.module.common.db.RealmSequenceProvider;
import net.bytle.tower.eraldy.module.list.model.ListGuid;
import net.bytle.tower.eraldy.module.organization.inputs.OrgaUserInputProps;
import net.bytle.tower.eraldy.module.organization.inputs.OrganizationInputProps;
import net.bytle.tower.eraldy.module.organization.model.OrgaGuid;
import net.bytle.tower.eraldy.module.organization.model.OrgaRole;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.tower.eraldy.module.organization.model.Organization;
import net.bytle.tower.eraldy.module.realm.inputs.RealmInputProps;
import net.bytle.tower.eraldy.module.realm.inputs.UserInputProps;
import net.bytle.tower.eraldy.module.realm.model.Realm;
import net.bytle.type.EmailAddress;
import net.bytle.type.Handle;
import net.bytle.type.UriEnhanced;
import net.bytle.type.Urls;
import net.bytle.vertx.ConfigAccessor;
import net.bytle.vertx.ConfigIllegalException;
import net.bytle.vertx.jackson.JacksonMapperManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * This class create the default Eraldy Model
 * (ie Organisation, Realm, App, User)
 */
public class EraldyModel {

  private static final Logger LOGGER = LogManager.getLogger(EraldyModel.class);

  /**
   * The URI of the interact app
   */
  private static final String INTERACT_APP_URI_CONF = "interact.app.uri";
  /**
   * The URI of the member app
   */
  private static final String MEMBER_APP_URI_CONF = "member.app.uri";
  /**
   * A template uri where the users can register publicly
   * (With the 2 %s as placeholder (the first one for the app guid
   * and the second for the list guid)
   * <p>
   * Example: `http(s)://domain.com/path/%s/%s`
   */
  private static final String MEMBER_REGISTRATION_URL = "member.list.registration.url.template";

  private static final long ORGA_LOCAL_ID = 1L;
  public static final long REALM_LOCAL_ID = 1L;
  private static final long OWNER_LOCAL_ID = 1L;
  private static final Long APP_MEMBER_ID = 1L;
  private static final Long APP_INTERACT_ID = 2L;
  private final EraldyApiApp apiApp;
  private final URI interactAppUri;
  private final URI memberAppUri;
  /**
   * A template URI with %s that is replaced by the list gui
   * This uri is where the users can register publicly to the list
   */
  private final String uriRegistrationPathTemplate;
  private final JacksonMapperManager jacksonManager;


  Realm eraldyRealm;
  private AuthClient memberClient;


  public EraldyModel(EraldyApiApp apiApp) throws ConfigIllegalException {

    this.apiApp = apiApp;

    /**
     * The owner is needed for authentication
     * We build it first
     * <p>
     * The realm is also used during initial insertion
     * of an organisation user at {@link #mount()}
     * It's updated just after in the {@link #mount()}
     *
     * We can't use the providers utility now
     * as they are initialized later, we create the guids manually then
     */
    this.eraldyRealm = Realm.createFromAnyId(ORGA_LOCAL_ID);
    this.eraldyRealm.setName("Eraldy");
    this.eraldyRealm.setHandle(Handle.ofFailSafe("eraldy"));

    /**
     * Orga Owner
     */
    OrgaGuid orgaGuid = new OrgaGuid();
    orgaGuid.setRealmId(this.eraldyRealm.getGuid().getLocalId());
    orgaGuid.setOrgaId(ORGA_LOCAL_ID);
    Organization ownerOrganization = Organization.createFromOrgGuid(orgaGuid);
    ownerOrganization.setName("Eraldy");
    ownerOrganization.setHandle(Handle.ofFailSafe("eraldy"));

    /**
     * User owner
     */
    OrgaUser orgaUserOwner = new OrgaUser();
    this.eraldyRealm.setOwnerUser(orgaUserOwner);
    OrgaUserGuid orgaUserGuid = new OrgaUserGuid();
    orgaUserGuid.setUserId(OWNER_LOCAL_ID);
    orgaUserGuid.setRealmId(ownerOrganization.getGuid().getRealmId());
    orgaUserGuid.setOrganizationId(ownerOrganization.getGuid().getOrgaId());
    orgaUserOwner.setGuid(orgaUserGuid);
    orgaUserOwner.setOrganization(ownerOrganization);
    orgaUserOwner.setGivenName("Nico");
    orgaUserOwner.setEmailAddress(EmailAddress.ofFailSafe("nico@eraldy.com"));
    try {
      orgaUserOwner.setAvatar(new URI("https://2.gravatar.com/avatar/cbc56a3848d90024bdc76629a1cfc1d9"));
    } catch (URISyntaxException e) {
      throw new RuntimeException("Owner Avatar uri is not valid", e);
    }
    orgaUserOwner.setOrganizationRole(OrgaRole.OWNER);
    orgaUserOwner.setRealm(this.eraldyRealm);


    this.jacksonManager = apiApp.getHttpServer().getServer().getJacksonMapperManager();
    ConfigAccessor configAccessor = apiApp.getHttpServer().getServer().getConfigAccessor();
    String interactUri = configAccessor.getString(INTERACT_APP_URI_CONF, "https://interact." + apiApp.getApexDomain().getUrlAuthority());
    try {
      this.interactAppUri = URI.create(interactUri);
      LOGGER.info("The interact app URI was set to ({}) via the conf ({})", interactUri, INTERACT_APP_URI_CONF);
    } catch (Exception e) {
      throw new ConfigIllegalException("The member app value (" + interactUri + ") of the conf (" + INTERACT_APP_URI_CONF + ") is not a valid URI", e);
    }

    /**
     * For redirect
     */
    String memberUri = configAccessor.getString(MEMBER_APP_URI_CONF, "https://member." + apiApp.getApexDomain().getUrlAuthority());
    try {
      this.memberAppUri = URI.create(memberUri);
      LOGGER.info("The member app URI was set to ({}) via the conf ({})", memberUri, MEMBER_APP_URI_CONF);
    } catch (Exception e) {
      throw new ConfigIllegalException("The member app value (" + memberUri + ") of the conf (" + MEMBER_APP_URI_CONF + ") is not a valid URI", e);
    }


    /**
     * List registration URL
     */
    this.uriRegistrationPathTemplate = configAccessor.getString(MEMBER_REGISTRATION_URL, memberUri + "/app/%s/list/%s/registration");
    try {
      String listGuid = "lis-yolo";
      String appGuidString = "app-youlou";
      URI uri = getMemberListRegistrationPath(appGuidString, listGuid);
      if (!uri.getPath().contains(listGuid) || !uri.getPath().contains(appGuidString)) {
        throw new ConfigIllegalException("The member registration url value (" + uriRegistrationPathTemplate + ") of the conf (" + MEMBER_REGISTRATION_URL + ") is not a valid URI template because it seems to not include 2 %s placeholder");
      }
    } catch (URISyntaxException e) {
      throw new ConfigIllegalException("The member registration url value (" + uriRegistrationPathTemplate + ") of the conf (" + MEMBER_REGISTRATION_URL + ") is not a valid URI template", e);
    }


  }

  public Future<Void> mount() {

    return deferredConnectionMount()
      .compose(v -> connectionMount());
  }


  /**
   * This is not a transaction
   * because you can't insert and then update the same record
   * It's then a problem when you insert 2 values
   * that uses our {@link RealmSequenceProvider sequence}
   * Below we insert 2 apps and the insert should be into 2 differents transactions
   */
  public Future<Void> connectionMount() {
    LOGGER.info("Get/Inserting the App and client");


    AppInputProps appInputProps = new AppInputProps();
    appInputProps.setName("Members");
    appInputProps.setHandle(Handle.ofFailSafe("members"));
    appInputProps.setHome(Urls.toUrlFailSafe("https://eraldy.com"));

    return this.apiApp.getAppProvider().getsertOnStartup(appInputProps, this.eraldyRealm, APP_MEMBER_ID)
      .compose(memberAppRes -> {
        /**
         * Create a client for the member App
         */
        memberClient = new AuthClient();
        memberClient.setApp(memberAppRes);
        memberClient.addUri(this.memberAppUri);
        AuthClientProvider authClientProvider = this.apiApp.getAuthClientProvider();
        authClientProvider.updateGuid(memberClient, 1L);
        authClientProvider.addEraldyClient(memberClient);
        String cliGuidHash = this.apiApp.getJackson().getSerializer(CliGuid.class).serialize(memberClient.getGuid());
        LOGGER.info("The client id (" + cliGuidHash + ") for the member app was created");

        AppInputProps interactApp = new AppInputProps();
        interactApp.setName("Interact");
        interactApp.setHandle(this.jacksonManager.getDeserializer(Handle.class).deserializeFailSafe("interact"));
        interactApp.setHome(Urls.toUrlFailSafe("https://eraldy.com"));
        return this.apiApp.getAppProvider().getsertOnStartup(interactApp, this.eraldyRealm, APP_INTERACT_ID);
      })
      .compose(interactAppRes -> {
        AuthClientProvider authClientProvider = this.apiApp.getAuthClientProvider();

        /**
         * Create a client for the interact App
         */
        AuthClient interactClient = new AuthClient();
        interactClient.setApp(interactAppRes);
        interactClient.addUri(this.interactAppUri);
        authClientProvider.updateGuid(interactClient, 2L);
        authClientProvider.addEraldyClient(interactClient);
        String cliGuidHash = this.apiApp.getJackson().getSerializer(CliGuid.class).serialize(interactClient.getGuid());
        LOGGER.info("The client id (" + cliGuidHash + ") for the interact app was created");

        return Future.succeededFuture();

      });


  }

  /**
   * When the data is inserted the first time,
   * - the user that owns the realm does not exist
   * - the realm does not exist but requires a user
   * We use a transaction to defer the constraint check
   * at the end (ie commit)
   */
  public Future<Void> deferredConnectionMount() {

    LOGGER.info("Get/Inserting the Eraldy Realm, Organization, and User");

    return apiApp.getHttpServer().getServer().getPostgresClient()
      .getPool()
      .withTransaction(sqlConnection -> sqlConnection
        /**
         * There are foreign-key circular constraints that cannot be resolved by simple insertion.
         * For instance,
         * - the owner of a realm must be an existing user
         * - but a user must have a realm
         * Without any data, there is no realm and no user, it's not possible to do 2 insertions
         * in isolation.
         * To resolve this problem, we insert all data at once (in one transaction)
         * and enforce the constraint at the end
         * This is what does the below statement
         * Note that all foreign-key circular constraints needs to be deferrable.
         * Example with an alter statement:
         * ALTER TABLE cs_realms.realm ALTER CONSTRAINT realm_organization_owner_user_fkey DEFERRABLE INITIALLY IMMEDIATE;
         */
        .query("SET CONSTRAINTS ALL DEFERRED")
        .execute()
        .compose(ar -> {

          /**
           * Realm First
           * (Cross, before updating the realm
           * need to be a property of the organizational user)
           */
          RealmInputProps realmInputProps = new RealmInputProps();
          realmInputProps.setHandle(this.eraldyRealm.getHandle());
          realmInputProps.setName(this.eraldyRealm.getName());
          realmInputProps.setOwnerUserGuid(this.eraldyRealm.getOwnerUser().getGuid());

          /**
           * Realm
           * We create the user as it's not yet in the db
           */
          UserInputProps realmOwnerInputProps = new UserInputProps();

          return this.apiApp.getRealmProvider()
            .getsertOnServerStartup(REALM_LOCAL_ID, realmInputProps, sqlConnection)
            .recover(t -> Future.failedFuture(new InternalException("Error while getserting the eraldy realm", t)))
            .compose(eraldyRealm -> {

              LOGGER.info("Eraldy Realm getserted");
              // Because we build objects only on row level (thanks to GraphQL)
              // it has lost the owner user, we set it back
              eraldyRealm.setOwnerUser(this.eraldyRealm.getOwnerUser());
              this.eraldyRealm = eraldyRealm;

              /**
               * Create if not exists and get the Eraldy Model
               * (ie getsert)
               */
              OrganizationInputProps organizationInputProps = new OrganizationInputProps();
              organizationInputProps.setHandle(this.eraldyRealm.getOwnerUser().getOrganization().getHandle());
              organizationInputProps.setName(this.eraldyRealm.getOwnerUser().getOrganization().getName());
              organizationInputProps.setOwnerUserGuid(this.eraldyRealm.getOwnerUser().getGuid());
              Future<Organization> futureOrganization = this.apiApp.getOrganizationProvider()
                .getsert(this.eraldyRealm, organizationInputProps, sqlConnection);

              /**
               * Composite of all organization data
               */
              return futureOrganization
                .compose(eraldyOrganization -> {

                  LOGGER.info("Eraldy Organization and roles getserted");

                  // orga is build lazily
                  this.eraldyRealm.getOwnerUser().setOrganization(eraldyOrganization);

                  OrgaUser ownerUser = eraldyRealm.getOwnerUser();
                  Future<OrgaUser> futureOwnerUser;
                  if (ownerUser != null && ownerUser.getGuid().getUserId() != OWNER_LOCAL_ID) {
                    /**
                     * If the user has changed, we take it instead
                     */
                    futureOwnerUser = Future.succeededFuture(ownerUser);
                  } else {
                    /**
                     * Realm Owner user getsertion
                     */
                    realmOwnerInputProps.setGivenName(this.eraldyRealm.getOwnerUser().getGivenName());
                    realmOwnerInputProps.setEmailAddress(this.eraldyRealm.getOwnerUser().getEmailAddress());
                    realmOwnerInputProps.setAvatar(this.eraldyRealm.getOwnerUser().getAvatar());

                    futureOwnerUser = apiApp
                      .getUserProvider()
                      .getsertOnServerStartup(eraldyRealm, OWNER_LOCAL_ID, realmOwnerInputProps, sqlConnection)
                      .recover(t -> Future.failedFuture(new InternalException("Error while getserting the eraldy owner user", t)))
                      .compose(eraldyRealmOwner -> {
                          LOGGER.info("Eraldy Realm Owner User getserted as Realm User");
                          /**
                           * get sert the user as organization user
                           */
                          OrgaUserInputProps orgaUserInputProps = new OrgaUserInputProps();
                          orgaUserInputProps.setRole(OrgaRole.OWNER);
                          return apiApp.getOrganizationUserProvider()
                            .getsertOnServerStartup(eraldyOrganization, eraldyRealmOwner, orgaUserInputProps, sqlConnection);
                        }
                      )
                      .recover(t -> Future.failedFuture(new InternalException("Error while getserting the eraldy owner organization user", t)));
                  }
                  return futureOwnerUser;
                });

            });
        })
      )
      .compose(realmOwnerUser -> {
        LOGGER.info("Eraldy Realm model loaded");
        if (realmOwnerUser == null) {
          return Future.failedFuture("The returned eraldy owner was null. Be sure to use the same sql connection on all operations");
        }
        eraldyRealm.setOwnerUser(realmOwnerUser);
        return Future.succeededFuture();
      });

  }


  public Realm getRealm() {
    return eraldyRealm;
  }

  public boolean isRealmLocalId(Long localId) {
    return Objects.equals(localId, this.eraldyRealm.getGuid().getLocalId());
  }


  public UriEnhanced getMemberAppUri() {

    return UriEnhanced.createFromUri(this.memberAppUri);

  }

  /**
   * A private utility function used at build time to test if the URI is valid.
   * That's why the signature does not have object such as the {@link ListObject} object but 2 strings
   * <p>
   * The public function {@link #getMemberListRegistrationPath(ListObject)} does have a {@link ListObject}
   * as signature
   */
  private URI getMemberListRegistrationPath(String appGuidHash, String listGuid) throws URISyntaxException {


    return new URI(String.format(this.uriRegistrationPathTemplate, appGuidHash, listGuid));

  }

  public URI getMemberListRegistrationPath(ListObject listObject) {
    String appGuidHash = this.jacksonManager.getSerializer(AppGuid.class).serialize(listObject.getApp().getGuid());
    String listGuidHash = this.jacksonManager.getSerializer(ListGuid.class).serialize(listObject.getGuid());
    try {
      return getMemberListRegistrationPath(appGuidHash, listGuidHash);
    } catch (URISyntaxException e) {
      throw new InternalException("The URI should have been tested at build time of Eraldy Model", e);
    }
  }


  public OrgaUser getOwnerUser() {
    return this.eraldyRealm.getOwnerUser();
  }

}
