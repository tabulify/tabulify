package net.bytle.tower;

import io.vertx.core.Future;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.objectProvider.AuthClientProvider;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.ConfigAccessor;
import net.bytle.vertx.ConfigIllegalException;
import net.bytle.vertx.TowerApexDomain;
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
  private final EraldyApiApp apiApp;
  private final URI interactAppUri;
  private final URI memberAppUri;
  /**
   * A template URI with %s that is replaced by the list gui
   * This uri is where the users can register publicly to the list
   */
  private final String uriRegistrationPathTemplate;

  Realm eraldyRealm;
  private AuthClient memberClient;


  public EraldyModel(EraldyApiApp apiApp) throws ConfigIllegalException {
    this.apiApp = apiApp;
    ConfigAccessor configAccessor = apiApp.getApexDomain().getHttpServer().getServer().getConfigAccessor();
    String interactUri = configAccessor.getString(INTERACT_APP_URI_CONF, "https://interact." + apiApp.getApexDomain().getApexNameWithPort());
    try {
      this.interactAppUri = URI.create(interactUri);
      LOGGER.info("The interact app URI was set to ({}) via the conf ({})", interactUri, INTERACT_APP_URI_CONF);
    } catch (Exception e) {
      throw new ConfigIllegalException("The member app value (" + interactUri + ") of the conf (" + INTERACT_APP_URI_CONF + ") is not a valid URI", e);
    }
    /**
     * For redirect
     */
    String memberUri = configAccessor.getString(MEMBER_APP_URI_CONF, "https://member." + apiApp.getApexDomain().getApexNameWithPort());
    try {
      this.memberAppUri = URI.create(memberUri);
      LOGGER.info("The member app URI was set to ({}) via the conf ({})", memberUri, MEMBER_APP_URI_CONF);
    } catch (Exception e) {
      throw new ConfigIllegalException("The member app value (" + memberUri + ") of the conf (" + MEMBER_APP_URI_CONF + ") is not a valid URI", e);
    }
    this.uriRegistrationPathTemplate = configAccessor.getString(MEMBER_REGISTRATION_URL, memberUri + "/app/%s/list/%s/registration");
    try {
      String listGuid = "lis-yolo";
      String appGuid = "app-youlou";
      URI uri = getMemberListRegistrationPath(appGuid, listGuid);
      if (!uri.getPath().contains(listGuid) || !uri.getPath().contains(appGuid)) {
        throw new ConfigIllegalException("The member registration url value (" + uriRegistrationPathTemplate + ") of the conf (" + MEMBER_REGISTRATION_URL + ") is not a valid URI template because it seems to not include 2 %s placeholder");
      }
    } catch (URISyntaxException e) {
      throw new ConfigIllegalException("The member registration url value (" + uriRegistrationPathTemplate + ") of the conf (" + MEMBER_REGISTRATION_URL + ") is not a valid URI template", e);
    }
  }


  public Future<Void> insertModelInDatabase() {

    LOGGER.info("Get/Inserting the Eraldy model");

    /**
     * When the data is inserted the first time,
     * - the user that owns the realm does not exist
     * - the realm does not exist but requires a user
     * We use a connection to defer the constraint check
     * at the end (ie commit)
     */
    return apiApp.getApexDomain().getHttpServer().getServer().getPostgresDatabaseConnectionPool()
      .withTransaction(sqlConnection-> sqlConnection
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
         * Example with a alter statement:
         * ALTER TABLE cs_realms.realm ALTER CONSTRAINT realm_organization_owner_user_fkey DEFERRABLE INITIALLY IMMEDIATE;
         */
        .query("SET CONSTRAINTS ALL DEFERRED")
        .execute()
        .compose(ar->{
          /**
           * Create if not exists and get the Eraldy Model
           * (ie getsert)
           */
          Organization initialEraldyOrganization = new Organization();
          initialEraldyOrganization.setLocalId(1L);
          initialEraldyOrganization.setHandle("eraldy");
          initialEraldyOrganization.setName("Eraldy");
          return this.apiApp.getOrganizationProvider()
            .getsert(initialEraldyOrganization, sqlConnection)
            .compose(eraldyOrganization -> {
              LOGGER.info("Eraldy Organization getserted");
              /**
               * Realm
               * (Cross, before updating the realm
               * need to be a property of the organizational user)
               */
              TowerApexDomain apexDomain = apiApp.getApexDomain();
              Realm initialEraldyRealm = new Realm();
              initialEraldyRealm.setHandle(apexDomain.getRealmHandle());
              initialEraldyRealm.setName(apexDomain.getName());
              initialEraldyRealm.setLocalId(this.getRealmLocalId());
              initialEraldyRealm.setOrganization(eraldyOrganization);

              /**
               * Organization User
               */
              OrganizationUser initialRealmOwnerUser = new OrganizationUser();
              initialRealmOwnerUser.setLocalId(1L);
              initialEraldyRealm.setOwnerUser(initialRealmOwnerUser);

              return this.apiApp.getRealmProvider()
                .getsert(initialEraldyRealm, sqlConnection)
                .recover(t -> Future.failedFuture(new InternalException("Error while getserting the eraldy realm", t)))
                .compose(eraldyRealm -> {
                  LOGGER.info("Eraldy Realm getserted");
                  this.eraldyRealm = eraldyRealm;

                  /**
                   * Realm Owner user getsertion
                   */
                  initialRealmOwnerUser.setRealm(eraldyRealm);
                  initialRealmOwnerUser.setGivenName(apexDomain.getOwnerName());
                  initialRealmOwnerUser.setEmail(apexDomain.getOwnerEmail());
                  initialRealmOwnerUser.setOrganization(eraldyOrganization);
                  try {
                    initialRealmOwnerUser.setAvatar(new URI("https://2.gravatar.com/avatar/cbc56a3848d90024bdc76629a1cfc1d9"));
                  } catch (URISyntaxException e) {
                    throw new InternalException("The eraldy owner URL is not valid", e);
                  }
                  return apiApp.getOrganizationUserProvider()
                    .getsert(initialRealmOwnerUser, sqlConnection)
                    .recover(t -> Future.failedFuture(new InternalException("Error while getserting the eraldy owner realm", t)))
                    .compose(realmOwnerUser -> {
                      LOGGER.info("Eraldy Realm Owner User getserted");
                      eraldyRealm.setOwnerUser(realmOwnerUser);

                      /**
                       * App getsertion
                       */
                      App memberApp = new App();
                      memberApp.setLocalId(1L);
                      memberApp.setName("Members");
                      memberApp.setHandle("members");
                      memberApp.setHome(URI.create("https://eraldy.com"));
                      memberApp.setUser(this.eraldyRealm.getOwnerUser()); // mandatory in the database (not null)
                      memberApp.setRealm(this.eraldyRealm);
                      Future<App> getSertMember = this.apiApp.getAppProvider().getsert(memberApp, sqlConnection);

                      App interactApp = new App();
                      interactApp.setLocalId(2L);
                      interactApp.setName("Interact");
                      interactApp.setHandle("interact");
                      interactApp.setHome(URI.create("https://eraldy.com"));
                      interactApp.setUser(this.eraldyRealm.getOwnerUser()); // mandatory in the database (not null)
                      interactApp.setRealm(this.eraldyRealm);
                      Future<App> getSertInteract = this.apiApp.getAppProvider().getsert(interactApp, sqlConnection);
                      return Future.all(getSertMember, getSertInteract);
                    })
                    .recover(t -> Future.failedFuture(new InternalException("Error while getserting the member and interact app", t)))
                    .compose(compositeResult -> {

                      App memberApp = compositeResult.resultAt(0);
                      App interactApp = compositeResult.resultAt(1);
                      AuthClientProvider authClientProvider = this.apiApp.getAuthClientProvider();

                      /**
                       * Create a client for the member App
                       */
                      memberClient = new AuthClient();
                      memberClient.setLocalId(1L);
                      memberClient.setApp(memberApp);
                      memberClient.addUri(this.memberAppUri);
                      authClientProvider.updateGuid(memberClient);
                      authClientProvider.addEraldyClient(memberClient);
                      LOGGER.info("The client id (" + memberClient.getGuid() + ") for the member app was created");

                      /**
                       * Create a client for the interact App
                       */
                      AuthClient interactClient = new AuthClient();
                      interactClient.setLocalId(2L);
                      interactClient.setApp(interactApp);
                      interactClient.addUri(this.interactAppUri);
                      authClientProvider.updateGuid(interactClient);
                      authClientProvider.addEraldyClient(interactClient);
                      LOGGER.info("The client id (" + interactClient.getGuid() + ") for the interact app was created");

                      return Future.succeededFuture();
                    });
                });
            });
        }));



  }

  public Long getRealmLocalId() {
    return this.apiApp.getApexDomain().getRealmLocalId();
  }

  public Realm getRealm() {
    return eraldyRealm;
  }

  public boolean isRealmLocalId(Long localId) {
    return Objects.equals(localId, apiApp.getApexDomain().getRealmLocalId());
  }

  public boolean isEraldyRealm(Realm realm) {
    return this.getRealmLocalId().equals(realm.getLocalId());
  }


  public UriEnhanced getMemberAppUri() {

    return UriEnhanced.createFromUri(this.memberAppUri);
  }

  public AuthClient getMemberClient() {
    return this.memberClient;
  }

  /**
   * A private utility function used at build time to test if the URI is valid.
   * That's why the signature does not have the {@link ListItem} object but 2 strings
   * <p>
   * The public function {@link #getMemberListRegistrationPath(ListItem)} does have a {@link ListItem}
   * as signature
   */
  private URI getMemberListRegistrationPath(String appGuid, String listGuid) throws URISyntaxException {

    return new URI(String.format(this.uriRegistrationPathTemplate, appGuid, listGuid));

  }

  public URI getMemberListRegistrationPath(ListItem listItem) {
    try {
      return getMemberListRegistrationPath(listItem.getOwnerApp().getGuid(), listItem.getGuid());
    } catch (URISyntaxException e) {
      throw new InternalException("The URI should have been tested at build time of Eraldy Model", e);
    }
  }

}
