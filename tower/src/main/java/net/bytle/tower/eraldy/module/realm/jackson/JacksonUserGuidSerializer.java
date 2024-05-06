package net.bytle.tower.eraldy.module.realm.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.tower.eraldy.module.realm.model.UserGuid;
import net.bytle.vertx.guid.GuidDeSer;
import net.bytle.vertx.jackson.JacksonJsonStringSerializer;

import java.io.IOException;

public class JacksonUserGuidSerializer extends JacksonJsonStringSerializer<UserGuid> {


  private final GuidDeSer guidDeSer;

  public JacksonUserGuidSerializer(GuidDeSer guidDeSer) {
    this.guidDeSer = guidDeSer;

  }

  @Override
  public void serialize(UserGuid value, JsonGenerator gen, SerializerProvider serializers) throws IOException {


    gen.writeString(serialize(value));

  }

  @Override
  public String serialize(UserGuid value) {
    return this.guidDeSer.serialize(value.getRealmId(), value.getLocalId());
  }
}
