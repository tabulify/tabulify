package net.bytle.tower.eraldy.module.app.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.tower.EraldyModel;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.module.app.db.AppProvider;
import net.bytle.tower.eraldy.module.app.model.AppGuid;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.HashId;
import net.bytle.vertx.jackson.JacksonJsonStringSerializer;

import java.io.IOException;

public class JacksonAppGuidSerializer extends JacksonJsonStringSerializer<AppGuid> {



  private final HashId hashIds;
  public JacksonAppGuidSerializer(EraldyApiApp apiApp) {
    this.hashIds = apiApp.getHttpServer().getServer().getHashId();

  }

  @Override
  public void serialize(AppGuid value, JsonGenerator gen, SerializerProvider serializers) throws IOException {


    gen.writeString(serialize(value));

  }

  @Override
  public String serialize(AppGuid value) {
    return Guid.builder(this.hashIds, AppProvider.APP_GUID_PREFIX)
      .setOrganizationOrRealmId(EraldyModel.REALM_LOCAL_ID)
      .setFirstObjectId(value.getLocalId())
      .build()
      .toString();
  }
}
