package net.bytle.tower;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.Organization;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.vertx.TowerApexDomain;

import java.net.URI;
import java.net.URISyntaxException;

public class EraldyRealm {

  private static EraldyRealm eraldyRealm;
  private final EraldyApiApp apiApp;

  Realm realm;

  public EraldyRealm(EraldyApiApp eraldyDomain) {
    this.apiApp = eraldyDomain;
  }

  public static EraldyRealm create(EraldyApiApp eraldyDomain) {
    eraldyRealm = new EraldyRealm(eraldyDomain);
    return eraldyRealm;
  }

  public static EraldyRealm get() {
    return eraldyRealm;
  }

  public Future<Realm> getFutureRealm() {
    /**
     * Update the Eraldy realm
     * Note: The eraldy realm already exists thanks to the database migration
     */
    Organization organization = new Organization();
    TowerApexDomain apexDomain = apiApp.getApexDomain();
    organization.setLocalId(apexDomain.getOrganisationId());

    realm = new Realm();
    realm.setHandle(apexDomain.getRealmHandle());
    realm.setName(apexDomain.getName());
    realm.setLocalId(apexDomain.getRealmLocalId());
    realm.setOrganization(organization);
    // TODO: create the user and role on organization level
    User ownerUser = new User();
    ownerUser.setName(apexDomain.getOwnerName());
    ownerUser.setEmail(apexDomain.getOwnerEmail());
    Realm clone = JsonObject.mapFrom(realm).mapTo(Realm.class);
    ownerUser.setRealm(clone); // to avoid recursion on com.fasterxml.jackson.databind
    try {
      ownerUser.setAvatar(new URI("https://2.gravatar.com/avatar/cbc56a3848d90024bdc76629a1cfc1d9"));
    } catch (URISyntaxException e) {
      throw new InternalException("The eraldy owner URL is not valid", e);
    }
    return apiApp.getUserProvider()
      .upsertUser(ownerUser)
      .onFailure(t -> {
        throw new InternalException("Error while creating the eraldy owner realm", t);
      })
      .compose(ownerDb -> {
        realm.setOwnerUser(ownerDb);
        return this.apiApp.getRealmProvider()
          .upsertRealm(realm)
          .onFailure(t -> {
              throw new InternalException("Error while creating the eraldy realm", t);
            }
          );
      })
      .compose(realmCompo -> {
        /**
         * To update the guid
         */
        realm.setGuid(realmCompo.getGuid());
        return Future.succeededFuture(realmCompo);
      });
  }

  public Realm getRealm() {
    return realm;
  }

}
