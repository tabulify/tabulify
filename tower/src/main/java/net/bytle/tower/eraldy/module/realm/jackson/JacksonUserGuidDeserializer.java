package net.bytle.tower.eraldy.module.realm.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.module.realm.model.UserGuid;
import net.bytle.vertx.guid.GuidDeSer;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;


public class JacksonUserGuidDeserializer extends JacksonJsonStringDeserializer<UserGuid> {

  private final GuidDeSer guidDeSer;

  public JacksonUserGuidDeserializer(GuidDeSer guidDeSer) {
    this.guidDeSer = guidDeSer;

  }

  @Override
  public UserGuid deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
      try {
          return deserialize(value);
      } catch (CastException e) {
          throw new IOException(e);
      }

  }

  @Override
  public UserGuid deserialize(String value) throws CastException {
    long[] userGuidObject;
    try {
      userGuidObject = this.guidDeSer.deserialize(value);
    } catch (CastException e) {
      throw new CastException("The user guid (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

    UserGuid userGuid = new UserGuid();
    long realmId = userGuidObject[0];
    userGuid.setRealmId(realmId);
    long localId = userGuidObject[1];
    userGuid.setUserId(localId);
    return userGuid;
  }
}
