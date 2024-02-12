package net.bytle.tower.util;

import io.vertx.core.Future;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.OrganizationUser;
import net.bytle.tower.eraldy.model.openapi.Realm;

public class EraldySubRealmModel {
  public static final String REALM_HANDLE = "datacadamia";
  private final EraldyApiApp apiApp;

  public EraldySubRealmModel(EraldyApiApp apiApp) {
    this.apiApp = apiApp;
  }

  public static EraldySubRealmModel getOrCreate(EraldyApiApp apiApp) {
    return new EraldySubRealmModel(apiApp);
  }

  public Future<Void> insertModelInDatabase() {

    Realm eraldyRealm = this.apiApp.getEraldyModel().getRealm();
    Realm datacadamiaRealm = new Realm();
    datacadamiaRealm.setHandle(REALM_HANDLE);
    datacadamiaRealm.setName(REALM_HANDLE + " Realm");
    datacadamiaRealm.setOrganization(eraldyRealm.getOrganization());
    OrganizationUser initialOwnerUser = new OrganizationUser();
    initialOwnerUser.setRealm(eraldyRealm);
    initialOwnerUser.setEmail("owner@datacadamia.com");

    return this.apiApp.getApexDomain().getHttpServer().getServer().getPostgresDatabaseConnectionPool()
      .withConnection(sqlConnection -> apiApp.getUserProvider()
        .getsertOnServerStartup(initialOwnerUser, sqlConnection, OrganizationUser.class)
        .recover(err->Future.failedFuture(new InternalException("Error on user getsert",err)))
        .compose(ownerUser -> {
          ownerUser.setOrganization(eraldyRealm.getOrganization());
          return apiApp.getOrganizationUserProvider()
            .getsertOnServerStartup(ownerUser, sqlConnection);
        })
        .recover(err->Future.failedFuture(new InternalException("Error on user organization getsert",err)))
        .compose(ownerResult -> {
            datacadamiaRealm.setOwnerUser(ownerResult);
            return this.apiApp.getRealmProvider()
              .getsertOnServerStartup(datacadamiaRealm, sqlConnection);
          }
        )
        .recover(err->Future.failedFuture(new InternalException("Error on realm getsert",err)))
        .compose(realm -> Future.succeededFuture()));


  }
}
