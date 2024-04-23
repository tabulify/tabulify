package net.bytle.tower.eraldy.module.user.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.module.user.db.UserProvider;
import net.bytle.tower.eraldy.module.user.model.UserGuid;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.HashId;

import java.io.IOException;

public class JacksonUserGuidSerializer extends JsonSerializer<UserGuid> {



  private final HashId hashIds;
  public JacksonUserGuidSerializer(EraldyApiApp apiApp) {
    this.hashIds = apiApp.getHttpServer().getServer().getHashId();

  }

  @Override
  public void serialize(UserGuid value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

    String guidHash = Guid.builder(this.hashIds, UserProvider.USR_GUID_PREFIX)
      .setOrganizationOrRealmId(value.getRealmId())
      .setFirstObjectId(value.getLocalId())
      .build()
      .toString();

    gen.writeString(guidHash);

  }

}
