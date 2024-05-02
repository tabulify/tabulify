package net.bytle.tower.eraldy.module.app.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.tower.EraldyModel;
import net.bytle.tower.eraldy.module.app.model.AppGuid;
import net.bytle.vertx.guid.GuidDeSer;
import net.bytle.vertx.jackson.JacksonJsonStringSerializer;

import java.io.IOException;

public class JacksonAppGuidSerializer extends JacksonJsonStringSerializer<AppGuid> {


  private final GuidDeSer guidDeSer;

  public JacksonAppGuidSerializer(GuidDeSer guidDeSer) {
    this.guidDeSer = guidDeSer;
  }

  @Override
  public void serialize(AppGuid value, JsonGenerator gen, SerializerProvider serializers) throws IOException {


    gen.writeString(serialize(value));

  }

  @Override
  public String serialize(AppGuid value) {
    return guidDeSer.serialize(EraldyModel.REALM_LOCAL_ID,value.getLocalId());
  }
}
