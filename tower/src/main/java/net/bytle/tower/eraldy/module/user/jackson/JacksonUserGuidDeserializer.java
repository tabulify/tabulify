package net.bytle.tower.eraldy.module.user.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.module.user.db.UserProvider;
import net.bytle.tower.eraldy.module.user.model.UserGuid;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.HashId;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;

import static net.bytle.tower.util.Guid.REALM_ID_OBJECT_ID_TYPE;

public class JacksonUserGuidDeserializer extends JacksonJsonStringDeserializer<UserGuid> {

  private final HashId hashIds;

  public JacksonUserGuidDeserializer(EraldyApiApp apiApp) {
    this.hashIds = apiApp.getHttpServer().getServer().getHashId();

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
    Guid userGuidObject;
    try {
      userGuidObject = new Guid.builder(this.hashIds, UserProvider.USR_GUID_PREFIX)
        .setCipherText(value, REALM_ID_OBJECT_ID_TYPE)
        .build();
    } catch (CastException e) {
      throw new CastException("The user guid (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }


    UserGuid userGuid = new UserGuid();
    long realmId = userGuidObject.getRealmOrOrganizationId();
    userGuid.setRealmId(realmId);
    long localId = userGuidObject.validateRealmAndGetFirstObjectId(realmId);
    userGuid.setLocalId(localId);
    return userGuid;
  }
}
