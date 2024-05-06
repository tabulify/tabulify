package net.bytle.tower.eraldy.module.realm.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.module.realm.model.RealmGuid;
import net.bytle.vertx.guid.GuidDeSer;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;

public class JacksonRealmGuidDeserializer extends JacksonJsonStringDeserializer<RealmGuid> {

  private final GuidDeSer guidDeSer;

  public JacksonRealmGuidDeserializer(GuidDeSer guidDeSer) {
    this.guidDeSer = guidDeSer;
  }

  @Override
  public RealmGuid deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
    try {
      return this.deserialize(value);
    } catch (CastException e) {
      throw new RuntimeException(e.getMessage(), e);
    }

  }

  public RealmGuid deserialize(String value) throws CastException {
    long[] ids;
    try {
      ids = guidDeSer.deserialize(value);
    } catch (CastException e) {
      throw new CastException("The realm guid (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }
    RealmGuid realmGuid = new RealmGuid(ids[0]);
    realmGuid.setHash(value);
    return realmGuid;
  }

}
