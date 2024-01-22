package net.bytle.tower;

import io.vertx.core.Future;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.Organization;
import net.bytle.tower.eraldy.model.openapi.OrganizationUser;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.vertx.TowerApexDomain;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * This class create the default Eraldy Model
 * (ie Organisation, Realm, App, User)
 */
public class EraldyModel {

  private final EraldyApiApp apiApp;

  Realm realm;

  public EraldyModel(EraldyApiApp eraldyDomain) {
    this.apiApp = eraldyDomain;
  }


  public Future<Void> insertModelInDatabase() {

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
    localRealm.setLocalId(apexDomain.getRealmLocalId());
    localRealm.setOrganization(organization);

    /**
     * Organization User
     */
    OrganizationUser ownerUser = new OrganizationUser();
    ownerUser.setLocalId(1L);
    ownerUser.setGivenName(apexDomain.getOwnerName());
    ownerUser.setEmail(apexDomain.getOwnerEmail());
    ownerUser.setRealm(localRealm);
    try {
      ownerUser.setAvatar(new URI("https://2.gravatar.com/avatar/cbc56a3848d90024bdc76629a1cfc1d9"));
    } catch (URISyntaxException e) {
      throw new InternalException("The eraldy owner URL is not valid", e);
    }
    return apiApp
      .getOrganizationUserProvider()
      .upsertUser(ownerUser)
      .recover(t -> Future.failedFuture(new InternalException("Error while creating the eraldy owner realm", t)))
      .compose(organizationUser -> {
        localRealm.setOwnerUser(organizationUser);
        return this.apiApp.getRealmProvider()
          .upsertRealm(localRealm);
      })
      .recover(t -> Future.failedFuture(new InternalException("Error while upserting the eraldy realm", t)))
      .compose(realmCompo -> {
        realm = realmCompo;
        return Future.succeededFuture();
      });
  }

  public Realm getRealm() {
    return realm;
  }

  public boolean isEraldyRealm(Long localId) {
    return Objects.equals(localId, apiApp.getApexDomain().getRealmLocalId());
  }
}
