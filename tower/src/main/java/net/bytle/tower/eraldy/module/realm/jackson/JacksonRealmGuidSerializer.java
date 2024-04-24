package net.bytle.tower.eraldy.module.realm.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.module.realm.db.RealmProvider;
import net.bytle.tower.eraldy.module.realm.model.RealmGuid;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.HashId;
import net.bytle.vertx.jackson.JacksonJsonStringSerializer;

import java.io.IOException;

public class JacksonRealmGuidSerializer extends JacksonJsonStringSerializer<RealmGuid> {



  private final HashId hashIds;
  public JacksonRealmGuidSerializer(EraldyApiApp apiApp) {
    this.hashIds = apiApp.getHttpServer().getServer().getHashId();

  }

  @Override
  public void serialize(RealmGuid value, JsonGenerator gen, SerializerProvider serializers) throws IOException {


    gen.writeString(serialize(value));

  }

  @Override
  public String serialize(RealmGuid value) {
    return Guid.builder(this.hashIds, RealmProvider.REALM_GUID_PREFIX)
      .setOrganizationOrRealmId(value.getLocalId())
      .build()
      .toString();
  }
}
