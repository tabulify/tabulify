package net.bytle.tower;

import io.vertx.core.Future;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.App;
import net.bytle.tower.eraldy.model.openapi.Organization;
import net.bytle.tower.eraldy.model.openapi.OrganizationUser;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.objectProvider.AuthClientProvider;
import net.bytle.tower.eraldy.objectProvider.OrganizationUserProvider;
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
   * A template uri with the %s as placeholder
   * where the users can register publicly
   * Example: `http(s)://domain.com/path/%s`
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

  Realm realm;
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
    this.uriRegistrationPathTemplate = configAccessor.getString(MEMBER_REGISTRATION_URL, memberUri + "/register/list/%s");
    try {
      String s = "lis-yolo";
      URI uri = getMemberListRegistrationPath(s);
      if (!uri.getPath().contains(s)) {
        throw new ConfigIllegalException("The member registration url value (" + uriRegistrationPathTemplate + ") of the conf (" + MEMBER_REGISTRATION_URL + ") is not a valid URI template because it seems to not include the %s placeholder");
      }
    } catch (Exception e) {
      throw new ConfigIllegalException("The member registration url value (" + uriRegistrationPathTemplate + ") of the conf (" + MEMBER_REGISTRATION_URL + ") is not a valid URI template", e);
    }
  }


  public Future<Void> insertModelInDatabase() {

    LOGGER.info("Get/Inserting the Eraldy model");

    /**
     * Update the Eraldy Model in the database
     * Note: The eraldy organisation and realm rows already exists thanks to the database migration script
     */
    Organization organization = new Organization();
    organization.setLocalId(1L);
    organization.setHandle("Eraldy");
    organization.setName("Eraldy");
    this.apiApp.getOrganizationProvider().updateGuid(organization);

    /**
     * Realm
     * (Cross, before updating the realm
     * need to be a property of the organizational user)
     */
    TowerApexDomain apexDomain = apiApp.getApexDomain();
    Realm localRealm = new Realm();
    localRealm.setHandle(apexDomain.getRealmHandle());
    localRealm.setName(apexDomain.getName());
    localRealm.setLocalId(this.getRealmLocalId());
    localRealm.setOrganization(organization);

    /**
     * Organization User
     */
    OrganizationUser ownerUser = new OrganizationUser();
    ownerUser.setLocalId(1L);
    ownerUser.setGivenName(apexDomain.getOwnerName());
    ownerUser.setEmail(apexDomain.getOwnerEmail());
    ownerUser.setRealm(localRealm);
    apiApp.getUserProvider().updateGuid(ownerUser);
    try {
      ownerUser.setAvatar(new URI("https://2.gravatar.com/avatar/cbc56a3848d90024bdc76629a1cfc1d9"));
    } catch (URISyntaxException e) {
      throw new InternalException("The eraldy owner URL is not valid", e);
    }
    OrganizationUserProvider organizationUserProvider = apiApp.getOrganizationUserProvider();
    return organizationUserProvider
      .getsert(ownerUser)
      .recover(t -> Future.failedFuture(new InternalException("Error while getserting the eraldy owner realm", t)))
      .compose(organizationUser -> {
        LOGGER.info("Owner User get/inserted");
        localRealm.setOwnerUser(organizationUser);
        return this.apiApp.getRealmProvider().getsert(localRealm);
      })
      .recover(t -> Future.failedFuture(new InternalException("Error while getserting the eraldy realm", t)))
      .compose(realmCompo -> {
        realm = realmCompo;
        App memberApp = new App();
        memberApp.setLocalId(1L);
        memberApp.setName("Members");
        memberApp.setHandle("members");
        memberApp.setHome(URI.create("https://eraldy.com"));
        memberApp.setUser(realm.getOwnerUser()); // mandatory in the database (not null)
        memberApp.setRealm(realm);
        Future<App> getSertMember = this.apiApp.getAppProvider().getsert(memberApp);

        App interactApp = new App();
        interactApp.setLocalId(2L);
        interactApp.setName("Interact");
        interactApp.setHandle("interact");
        interactApp.setHome(URI.create("https://eraldy.com"));
        interactApp.setUser(realm.getOwnerUser()); // mandatory in the database (not null)
        interactApp.setRealm(realm);
        Future<App> getSertInteract = this.apiApp.getAppProvider().getsert(interactApp);
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
  }

  public Long getRealmLocalId() {
    return this.apiApp.getApexDomain().getRealmLocalId();
  }

  public Realm getRealm() {
    return realm;
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

  public URI getMemberListRegistrationPath(String s) {
    try {
      return new URI(String.format(this.uriRegistrationPathTemplate, s));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
