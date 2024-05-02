package net.bytle.tower.eraldy.module.organization.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.module.organization.model.OrgaGuid;
import net.bytle.vertx.guid.GuidDeSer;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;

public class JacksonOrgaGuidDeserializer extends JacksonJsonStringDeserializer<OrgaGuid> {

  private final GuidDeSer guidDeSer;

  public JacksonOrgaGuidDeserializer(GuidDeSer guidDeSer) {
    this.guidDeSer = guidDeSer;
  }

  @Override
  public OrgaGuid deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
      try {
          return this.deserialize(value);
      } catch (CastException e) {
          throw new RuntimeException(e.getMessage(),e);
      }

  }

  public OrgaGuid deserialize(String value) throws CastException {
    long[] ids;
    try {
      ids = guidDeSer.deserialize(value);
    } catch (CastException e) {
      throw new CastException("The orga guid (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

    OrgaGuid orgaGuidObject = new OrgaGuid(ids[0]);
    orgaGuidObject.setPublicHash(value);
    return orgaGuidObject;

  }



}
