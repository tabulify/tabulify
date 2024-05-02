package net.bytle.tower.eraldy.module.auth.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.tower.eraldy.module.auth.model.CliGuid;
import net.bytle.vertx.guid.GuidDeSer;
import net.bytle.vertx.jackson.JacksonJsonStringSerializer;

import java.io.IOException;

public class JacksonCliGuidSerializer extends JacksonJsonStringSerializer<CliGuid> {


  private final GuidDeSer guidDeSer;

  public JacksonCliGuidSerializer(GuidDeSer guidDeSer) {
    this.guidDeSer = guidDeSer;
  }

  @Override
  public void serialize(CliGuid value, JsonGenerator gen, SerializerProvider serializers) throws IOException {


    gen.writeString(serialize(value));

  }

  @Override
  public String serialize(CliGuid value) {
    return guidDeSer.serialize(value.getRealmId(),value.getLocalId());
  }
}
