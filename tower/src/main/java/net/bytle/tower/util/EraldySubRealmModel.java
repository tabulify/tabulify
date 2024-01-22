package net.bytle.tower.util;

import io.vertx.core.Future;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;

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
    User owner = new User();
    owner.setRealm(eraldyRealm);
    owner.setEmail("owner@datacadamia.com");
    return apiApp.getUserProvider()
      .upsertUser(owner)
      .compose(ownerResult -> this.apiApp.getRealmProvider()
        .upsertRealm(datacadamiaRealm)
      )
      .compose(realm -> Future.succeededFuture());

  }
}
