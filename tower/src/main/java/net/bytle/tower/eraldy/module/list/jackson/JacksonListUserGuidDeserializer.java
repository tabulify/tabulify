package net.bytle.tower.eraldy.module.list.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.module.list.model.ListUserGuid;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.HashId;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;

import static net.bytle.tower.util.Guid.REALM_ID_TWO_OBJECT_ID_TYPE;

public class JacksonListUserGuidDeserializer extends JacksonJsonStringDeserializer<ListUserGuid> {

  private final HashId hashIds;

  public JacksonListUserGuidDeserializer(EraldyApiApp apiApp) {
    this.hashIds = apiApp.getHttpServer().getServer().getHashId();

  }

  @Override
  public ListUserGuid deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
    try {
      return deserialize(value);
    } catch (CastException e) {
      throw new IOException("The list user guid value (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

  }


  @Override
  public ListUserGuid deserialize(String value) throws CastException {
    Guid userGuidObject;
    try {
      userGuidObject = new Guid.builder(this.hashIds, ListUserGuid.GUID_PREFIX)
        .setCipherText(value, REALM_ID_TWO_OBJECT_ID_TYPE)
        .build();
    } catch (CastException e) {
      throw new CastException("The list guid (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

    ListUserGuid listGuid = new ListUserGuid();
    long realmId = userGuidObject.getRealmOrOrganizationId();
    listGuid.setRealmId(realmId);
    long listId = userGuidObject.validateRealmAndGetFirstObjectId(realmId);
    listGuid.setListId(listId);
    long userId = userGuidObject.validateAndGetSecondObjectId(realmId);
    listGuid.setUserId(userId);
    return listGuid;
  }
}
