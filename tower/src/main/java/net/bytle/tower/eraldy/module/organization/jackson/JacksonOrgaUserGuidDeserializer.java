package net.bytle.tower.eraldy.module.organization.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.vertx.guid.GuidDeSer;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;

public class JacksonOrgaUserGuidDeserializer extends JacksonJsonStringDeserializer<OrgaUserGuid> {

  private final GuidDeSer guidDeSer;

  public JacksonOrgaUserGuidDeserializer(GuidDeSer guidDeSer) {
    this.guidDeSer = guidDeSer;
  }

  @Override
  public OrgaUserGuid deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
    try {
      return this.deserialize(value);
    } catch (CastException e) {
      throw new RuntimeException(e.getMessage(), e);
    }

  }

  public OrgaUserGuid deserialize(String value) throws CastException {
    long[] ids;
    try {
      ids = this.guidDeSer.deserialize(value);
    } catch (CastException e) {
      throw new CastException("The user guid (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

    OrgaUserGuid orgaUserGuid = new OrgaUserGuid.Builder()
      .setRealmId(ids[0])
      .setOrgaId(ids[1])
      .setUserId(ids[2])
      .build();
    orgaUserGuid.setHash(value);
    return orgaUserGuid;
  }

}
