package net.bytle.tower;

import io.vertx.core.Future;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.App;
import net.bytle.tower.eraldy.model.openapi.Organization;
import net.bytle.tower.eraldy.model.openapi.OrganizationUser;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.objectProvider.OrganizationUserProvider;
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
  private final EraldyApiApp apiApp;
  private final URI interactAppUri;

  Realm realm;
  private App interactApp;

  public EraldyModel(EraldyApiApp apiApp) throws ConfigIllegalException {
    this.apiApp = apiApp;
    String interactUri = apiApp.getApexDomain().getHttpServer().getServer().getConfigAccessor().getString(INTERACT_APP_URI_CONF, "https://interact." + apiApp.getApexDomain().getApexNameWithPort());
    try {
      this.interactAppUri = URI.create(interactUri);
      LOGGER.info("The interact app URI was set to ({}) via the conf ({})", interactUri, INTERACT_APP_URI_CONF);
    } catch (Exception e) {
      throw new ConfigIllegalException("The member app value (" + interactUri + ") of the conf (" + INTERACT_APP_URI_CONF + ") is not a valid URI", e);
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
        App interactApp = new App();
        interactApp.setLocalId(1L);
        interactApp.setName("Interact");
        interactApp.setHandle("interact");
        interactApp.setHome(URI.create("https://eraldy.com"));
        interactApp.setUser(realm.getOwnerUser()); // mandatory in the database (not null)
        interactApp.setRealm(realm);
        return this.apiApp.getAppProvider().getsert(interactApp);
      })
      .recover(t -> Future.failedFuture(new InternalException("Error while getserting the interact app", t)))
      .compose(resApp -> {
        this.interactApp = resApp;
        /**
         * Create a client for the App
         */
        ApiClient interactClient = new ApiClient();
        interactClient.setLocalId(1L);
        interactClient.setApp(this.interactApp);
        interactClient.addUri(this.interactAppUri);
        this.apiApp.getApiClientProvider()
          .setInteractAppClient(interactClient);
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



}
