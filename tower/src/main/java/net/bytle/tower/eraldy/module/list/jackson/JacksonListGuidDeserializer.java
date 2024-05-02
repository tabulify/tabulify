package net.bytle.tower.eraldy.module.list.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.module.list.model.ListGuid;
import net.bytle.vertx.guid.GuidDeSer;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;

public class JacksonListGuidDeserializer extends JacksonJsonStringDeserializer<ListGuid> {

  private final GuidDeSer guidDeSer;

  public JacksonListGuidDeserializer(GuidDeSer guidDeSer) {
    this.guidDeSer = guidDeSer;

  }

  @Override
  public ListGuid deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
    try {
      return deserialize(value);
    } catch (CastException e) {
      throw new IOException("The list guid value (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

  }


  @Override
  public ListGuid deserialize(String value) throws CastException {
    long[] ids;
    try {
      ids = guidDeSer.deserialize(value);
    } catch (CastException e) {
      throw new CastException("The list guid (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

    ListGuid listGuid = new ListGuid();
    long realmId = ids[0];
    listGuid.setRealmId(realmId);
    long localId = ids[1];
    listGuid.setLocalId(localId);
    return listGuid;
  }
}
