package net.bytle.tower.eraldy.module.organization.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.tower.eraldy.module.organization.model.OrgaGuid;
import net.bytle.vertx.guid.GuidDeSer;
import net.bytle.vertx.jackson.JacksonJsonStringSerializer;

import java.io.IOException;

public class JacksonOrgaGuidSerializer extends JacksonJsonStringSerializer<OrgaGuid> {



  private final GuidDeSer guidDeSer;
  public JacksonOrgaGuidSerializer(GuidDeSer guidDeSer) {
    this.guidDeSer = guidDeSer;
  }

  @Override
  public void serialize(OrgaGuid value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

    gen.writeString(serialize(value));

  }

  @Override
  public String serialize(OrgaGuid orgaGuid) {
    return guidDeSer.serialize(orgaGuid.getLocalId());
  }
}
