package net.bytle.tower.eraldy.module.organization.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.tower.EraldyModel;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.vertx.guid.GuidDeSer;
import net.bytle.vertx.jackson.JacksonJsonStringSerializer;

import java.io.IOException;

public class JacksonOrgaUserGuidSerializer extends JacksonJsonStringSerializer<OrgaUserGuid> {


  private final GuidDeSer guidDeSer;

  public JacksonOrgaUserGuidSerializer(GuidDeSer guidDeSer) {
    this.guidDeSer = guidDeSer;

  }

  @Override
  public void serialize(OrgaUserGuid value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

    gen.writeString(serialize(value));

  }

  @Override
  public String serialize(OrgaUserGuid value) {
    return guidDeSer.serialize(
      EraldyModel.REALM_LOCAL_ID,
      value.getLocalId(),
      value.getOrganizationId()
    );
  }
}
