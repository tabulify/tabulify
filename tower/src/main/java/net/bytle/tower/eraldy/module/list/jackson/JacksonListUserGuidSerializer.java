package net.bytle.tower.eraldy.module.list.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.tower.eraldy.module.list.model.ListUserGuid;
import net.bytle.vertx.guid.GuidDeSer;
import net.bytle.vertx.jackson.JacksonJsonStringSerializer;

import java.io.IOException;

public class JacksonListUserGuidSerializer extends JacksonJsonStringSerializer<ListUserGuid> {


  private final GuidDeSer guidDeSer;

  public JacksonListUserGuidSerializer(GuidDeSer guidDeSer) {
    this.guidDeSer = guidDeSer;
  }

  @Override
  public void serialize(ListUserGuid value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

    gen.writeString(serialize(value));

  }

  @Override
  public String serialize(ListUserGuid value) {
    return guidDeSer.serialize(
      value.getRealmId(),
      value.getListId(),
      value.getUserId()
    );
  }
}
