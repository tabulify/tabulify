package net.bytle.tower.eraldy.module.list.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.module.list.model.ListUserGuid;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.HashId;
import net.bytle.vertx.jackson.JacksonJsonStringSerializer;

import java.io.IOException;

public class JacksonListUserGuidSerializer extends JacksonJsonStringSerializer<ListUserGuid> {



  private final HashId hashIds;
  public JacksonListUserGuidSerializer(EraldyApiApp apiApp) {
    this.hashIds = apiApp.getHttpServer().getServer().getHashId();

  }

  @Override
  public void serialize(ListUserGuid value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

    gen.writeString(serialize(value));

  }

  @Override
  public String serialize(ListUserGuid value) {
    return Guid.builder(this.hashIds, ListUserGuid.GUID_PREFIX)
      .setOrganizationOrRealmId(value.getRealmId())
      .setFirstObjectId(value.getListId())
      .setSecondObjectId(value.getUserId())
      .build()
      .toString();
  }
}
