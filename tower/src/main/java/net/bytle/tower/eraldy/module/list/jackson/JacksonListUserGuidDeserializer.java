package net.bytle.tower.eraldy.module.list.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.module.list.model.ListUserGuid;
import net.bytle.vertx.guid.GuidDeSer;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;

public class JacksonListUserGuidDeserializer extends JacksonJsonStringDeserializer<ListUserGuid> {

  private final GuidDeSer guidDeSer;

  public JacksonListUserGuidDeserializer(GuidDeSer guidDeSer) {
    this.guidDeSer = guidDeSer;

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
    long[] userGuidObject;
    try {
      userGuidObject = this.guidDeSer.deserialize(value);
    } catch (CastException e) {
      throw new CastException("The list user guid (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

    ListUserGuid listGuid = new ListUserGuid();
    long realmId = userGuidObject[0];
    listGuid.setRealmId(realmId);
    long listId = userGuidObject[1];
    listGuid.setListId(listId);
    long userId = userGuidObject[2];
    listGuid.setUserId(userId);
    listGuid.setHash(value);
    return listGuid;
  }
}
