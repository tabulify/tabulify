package net.bytle.tower.eraldy.module.app.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.module.app.model.AppGuid;
import net.bytle.vertx.guid.GuidDeSer;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;

public class JacksonAppGuidDeserializer extends JacksonJsonStringDeserializer<AppGuid> {


  private final GuidDeSer guidDeSer;

  public JacksonAppGuidDeserializer(GuidDeSer guidDeSer) {
    this.guidDeSer = guidDeSer;

  }

  @Override
  public AppGuid deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
    try {
      return deserialize(value);
    } catch (CastException e) {
      throw new IOException(e);
    }

  }


  @Override
  public AppGuid deserialize(String value) throws CastException {
    long[] userGuidObject;
    try {
      userGuidObject = this.guidDeSer.deserialize(value);
    } catch (CastException e) {
      throw new CastException("The app guid (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }
    AppGuid appGuid = new AppGuid();
    long realmId = userGuidObject[0];
    appGuid.setRealmId(realmId);
    long localId = userGuidObject[1];
    appGuid.setLocalId(localId);
    appGuid.setHash(value);
    return appGuid;
  }
}
