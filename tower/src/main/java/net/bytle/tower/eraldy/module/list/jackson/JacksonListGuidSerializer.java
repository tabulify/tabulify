package net.bytle.tower.eraldy.module.list.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.module.list.db.ListProvider;
import net.bytle.tower.eraldy.module.list.model.ListGuid;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.HashId;
import net.bytle.vertx.jackson.JacksonJsonStringSerializer;

import java.io.IOException;

public class JacksonListGuidSerializer extends JacksonJsonStringSerializer<ListGuid> {



  private final HashId hashIds;
  public JacksonListGuidSerializer(EraldyApiApp apiApp) {
    this.hashIds = apiApp.getHttpServer().getServer().getHashId();

  }

  @Override
  public void serialize(ListGuid value, JsonGenerator gen, SerializerProvider serializers) throws IOException {


    gen.writeString(serialize(value));

  }

  @Override
  public String serialize(ListGuid value) {
    return Guid.builder(this.hashIds, ListProvider.LIST_GUID_PREFIX)
      .setOrganizationOrRealmId(value.getRealmId())
      .setFirstObjectId(value.getLocalId())
      .build()
      .toString();
  }
}
