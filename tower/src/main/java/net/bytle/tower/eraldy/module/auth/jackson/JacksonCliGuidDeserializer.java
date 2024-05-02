package net.bytle.tower.eraldy.module.auth.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.module.auth.model.CliGuid;
import net.bytle.vertx.guid.GuidDeSer;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;

public class JacksonCliGuidDeserializer extends JacksonJsonStringDeserializer<CliGuid> {


  private final GuidDeSer guidDeSer;

  public JacksonCliGuidDeserializer(GuidDeSer guidDeSer) {
    this.guidDeSer = guidDeSer;

  }

  @Override
  public CliGuid deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
    try {
      return deserialize(value);
    } catch (CastException e) {
      throw new IOException(e);
    }

  }


  @Override
  public CliGuid deserialize(String value) throws CastException {
    long[] userGuidObject;
    try {
      userGuidObject = this.guidDeSer.deserialize(value);
    } catch (CastException e) {
      throw new CastException("The cli guid (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }
    CliGuid cliGuid = new CliGuid();
    long realmId = userGuidObject[0];
    cliGuid.setRealmId(realmId);
    long localId = userGuidObject[1];
    cliGuid.setLocalId(localId);
    return cliGuid;
  }
}
