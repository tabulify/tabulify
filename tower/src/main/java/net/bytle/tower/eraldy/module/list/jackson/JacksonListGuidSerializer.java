package net.bytle.tower.eraldy.module.list.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.tower.eraldy.module.list.model.ListGuid;
import net.bytle.vertx.guid.GuidDeSer;
import net.bytle.vertx.jackson.JacksonJsonStringSerializer;

import java.io.IOException;

public class JacksonListGuidSerializer extends JacksonJsonStringSerializer<ListGuid> {


  private final GuidDeSer guidDeSer;

  public JacksonListGuidSerializer(GuidDeSer guidDeSer) {
    this.guidDeSer = guidDeSer;

  }

  @Override
  public void serialize(ListGuid value, JsonGenerator gen, SerializerProvider serializers) throws IOException {


    gen.writeString(serialize(value));

  }

  @Override
  public String serialize(ListGuid value) {
    return this.guidDeSer.serialize(value.getRealmId(), value.getLocalId());
  }

}
