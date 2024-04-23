package net.bytle.tower;

import io.vertx.core.Future;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.module.app.model.AppGuid;
import net.bytle.tower.eraldy.module.organization.inputs.OrgaUserInputProps;
import net.bytle.tower.eraldy.module.organization.inputs.OrganizationInputProps;
import net.bytle.tower.eraldy.module.organization.model.OrgaRole;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.tower.eraldy.module.realm.inputs.RealmInputProps;
import net.bytle.tower.eraldy.module.user.inputs.UserInputProps;
import net.bytle.tower.eraldy.objectProvider.AuthClientProvider;
import net.bytle.type.Handle;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.ConfigAccessor;
import net.bytle.vertx.ConfigIllegalException;
import net.bytle.vertx.TowerApexDomain;
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

    /**
     * The realm is used during initial insertion
     * of an organisation user at {@link #mount()}
     * It's updated just after in the {@link #mount()}
     */
    Organization organization = new Organization();
    organization.setLocalId(ORGA_LOCAL_ID);
    this.eraldyRealm = new Realm();
    this.eraldyRealm.setLocalId(REALM_LOCAL_ID);
    this.eraldyRealm.setOrganization(organization);

  }

  public Future<Void> mount() {

    return deferredConnectionMount()
      .compose(v -> connectionMount());
  }


  /**
   * This is not a transaction
   * because you can't insert and then update the same record
   * It's then a problem when you insert 2 values
   * that uses our {@link net.bytle.tower.eraldy.objectProvider.RealmSequenceProvider sequence}
   * Below we insert 2 apps and the insert should be into 2 differents transactions
   */
  public Future<Void> connectionMount() {
    LOGGER.info("Get/Inserting the App and client");


    App memberApp = new App();
    memberApp.setLocalId(APP_MEMBER_ID);
    memberApp.setName("Members");
    memberApp.setHandle(this.jacksonManager.getDeserializer(Handle.class).deserializeFailSafe("members"));
    memberApp.setHome(URI.create("https://eraldy.com"));
    memberApp.setOwnerUser(this.eraldyRealm.getOwnerUser()); // mandatory in the database (not null)
    memberApp.setRealm(this.eraldyRealm);
    return this.apiApp.getAppProvider().getsertOnStartup(memberApp)
      .compose(memberAppRes -> {
        /**
         * Create a client for the member App
         */
        memberClient = new AuthClient();
        memberClient.setLocalId(1L);
        memberClient.setApp(memberAppRes);
        memberClient.addUri(this.memberAppUri);
        AuthClientProvider authClientProvider = this.apiApp.getAuthClientProvider();
        authClientProvider.updateGuid(memberClient);
        authClientProvider.addEraldyClient(memberClient);
        LOGGER.info("The client id (" + memberClient.getGuid() + ") for the member app was created");

        App interactApp = new App();
        interactApp.setLocalId(APP_INTERACT_ID);
        interactApp.setName("Interact");
        interactApp.setHandle(this.jacksonManager.getDeserializer(Handle.class).deserializeFailSafe("interact"));
        interactApp.setHome(URI.create("https://eraldy.com"));
        interactApp.setOwnerUser(this.eraldyRealm.getOwnerUser()); // mandatory in the database (not null)
        interactApp.setRealm(this.eraldyRealm);
        return this.apiApp.getAppProvider().getsertOnStartup(interactApp);
      })
      .compose(interactAppRes -> {
        AuthClientProvider authClientProvider = this.apiApp.getAuthClientProvider();

        /**
         * Create a client for the interact App
         */
        AuthClient interactClient = new AuthClient();
        interactClient.setLocalId(2L);
        interactClient.setApp(interactAppRes);
        interactClient.addUri(this.interactAppUri);
        authClientProvider.updateGuid(interactClient);
        authClientProvider.addEraldyClient(interactClient);
        LOGGER.info("The client id (" + interactClient.getGuid() + ") for the interact app was created");

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

    LOGGER.info("Get/Inserting the Eraldy Organization, Realm and User");

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
           * Owner in GUID
           */
          OrgaUserGuid eraldyOwnerUserGuid = new OrgaUserGuid();
          eraldyOwnerUserGuid.setLocalId(OWNER_LOCAL_ID);


          OrgaUser realmOwner = new OrgaUser();
          realmOwner.setLocalId(OWNER_LOCAL_ID);
          Realm realmForRealmOwner = new Realm();
          realmForRealmOwner.setLocalId(REALM_LOCAL_ID);
          realmOwner.setRealm(realmForRealmOwner);
          realmOwner.setOrgaRole(OrgaRole.OWNER);

          /**
           * Create if not exists and get the Eraldy Model
           * (ie getsert)
           */
          OrganizationInputProps organizationInputProps = new OrganizationInputProps();
          organizationInputProps.setHandle("eraldy");
          organizationInputProps.setName("Eraldy");
          organizationInputProps.setOwnerGuid(eraldyOwnerUserGuid);
          Future<Organization> futureOrganization = this.apiApp.getOrganizationProvider()
            .getsert(ORGA_LOCAL_ID, organizationInputProps, sqlConnection);

          /**
           * The organization roles
           */
          Future<Void> upsertAllRoles = this.apiApp.getOrganizationRoleProvider().upsertAll(sqlConnection);

          /**
           * Composite of all organization data
           */
          return Future.all(
              futureOrganization,
              upsertAllRoles
            )
            .compose(composite -> {
              LOGGER.info("Eraldy Organization and roles getserted");

              Organization eraldyOrganization = composite.resultAt(0);
              realmOwner.setOrganization(eraldyOrganization);

              /**
               * Realm
               * (Cross, before updating the realm
               * need to be a property of the organizational user)
               */
              TowerApexDomain apexDomain = apiApp.getApexDomain();
              RealmInputProps realmInputProps = new RealmInputProps();
              realmInputProps.setHandle(apexDomain.getRealmHandle());
              realmInputProps.setName(apexDomain.getName());
              realmInputProps.setOwnerUserGuid(eraldyOwnerUserGuid);

              /**
               * Realm
               * We create nep objects
               */
              UserInputProps realmOwnerInputProps = new UserInputProps();

              return this.apiApp.getRealmProvider()
                .getsertOnServerStartup(REALM_LOCAL_ID, realmOwner, realmInputProps, sqlConnection)
                .recover(t -> Future.failedFuture(new InternalException("Error while getserting the eraldy realm", t)))
                .compose(eraldyRealm -> {
                  LOGGER.info("Eraldy Realm getserted");
                  this.eraldyRealm = eraldyRealm;

                  OrgaUser ownerUser = eraldyRealm.getOwnerUser();
                  Future<OrgaUser> futureOwnerUser;
                  if (ownerUser != null && !ownerUser.getLocalId().equals(OWNER_LOCAL_ID)) {
                    /**
                     * If the user has changed, we take it instead
                     */
                    futureOwnerUser = Future.succeededFuture(ownerUser);
                  } else {
                    /**
                     * Realm Owner user getsertion
                     */
                    realmOwnerInputProps.setGivenName(apexDomain.getOwnerName());
                    realmOwnerInputProps.setEmailAddress(apexDomain.getOwnerEmail());
                    try {
                      realmOwnerInputProps.setAvatar(new URI("https://2.gravatar.com/avatar/cbc56a3848d90024bdc76629a1cfc1d9"));
                    } catch (URISyntaxException e) {
                      throw new InternalException("The eraldy owner URL is not valid", e);
                    }
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
        eraldyRealm.setOwnerUser(realmOwnerUser);
        return Future.succeededFuture();
      });

  }


  public Realm getRealm() {
    return eraldyRealm;
  }

  public boolean isRealmLocalId(Long localId) {
    return Objects.equals(localId, apiApp.getApexDomain().getRealmLocalId());
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
    try {
      return getMemberListRegistrationPath(appGuidHash, listObject.getGuid());
    } catch (URISyntaxException e) {
      throw new InternalException("The URI should have been tested at build time of Eraldy Model", e);
    }
  }

}
