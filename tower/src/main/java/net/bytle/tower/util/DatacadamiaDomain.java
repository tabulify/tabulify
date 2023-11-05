package net.bytle.tower.util;

import io.vertx.core.Future;
import net.bytle.tower.EraldyRealm;
import net.bytle.tower.VerticleApi;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;

public class DatacadamiaDomain {
  public static final String REALM_HANDLE = "datacadamia";
  private final VerticleApi verticle;

  public DatacadamiaDomain(VerticleApi verticleHttpServer) {
    this.verticle = verticleHttpServer;
  }

  public static DatacadamiaDomain getOrCreate(VerticleApi verticleHttpServer) {
    return new DatacadamiaDomain(verticleHttpServer);
  }

  public Future<Realm> createRealm() {

    Realm eraldyRealm = EraldyRealm.get().getRealm();
    Realm datacadamiaRealm = new Realm();
    datacadamiaRealm.setHandle(REALM_HANDLE);
    datacadamiaRealm.setName(REALM_HANDLE + " Realm");
    datacadamiaRealm.setOrganization(eraldyRealm.getOrganization());
    User owner = new User();
    owner.setRealm(eraldyRealm);
    owner.setEmail("owner@datacadamia.com");
    return verticle.getApp().getUserProvider()
      .upsertUser(owner)
      .compose(ownerResult -> this.verticle.getApp().getRealmProvider()
        .upsertRealm(datacadamiaRealm));

  }
}
