package net.bytle.tower.eraldy.module.list.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.module.list.db.ListProvider;
import net.bytle.tower.eraldy.module.list.model.ListGuid;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.HashId;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;

import static net.bytle.tower.util.Guid.REALM_ID_OBJECT_ID_TYPE;

public class JacksonListGuidDeserializer extends JacksonJsonStringDeserializer<ListGuid> {

  private final HashId hashIds;

  public JacksonListGuidDeserializer(EraldyApiApp apiApp) {
    this.hashIds = apiApp.getHttpServer().getServer().getHashId();

  }

  @Override
  public ListGuid deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
    try {
      return deserialize(value);
    } catch (CastException e) {
      throw new IOException(e);
    }

  }


  @Override
  public ListGuid deserialize(String value) throws CastException {
    Guid userGuidObject;
    try {
      userGuidObject = new Guid.builder(this.hashIds, ListProvider.LIST_GUID_PREFIX)
        .setCipherText(value, REALM_ID_OBJECT_ID_TYPE)
        .build();
    } catch (CastException e) {
      throw new CastException("The list guid (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

    ListGuid appGuid = new ListGuid();
    long realmId = userGuidObject.getRealmOrOrganizationId();
    appGuid.setRealmId(realmId);
    long localId = userGuidObject.validateRealmAndGetFirstObjectId(realmId);
    appGuid.setLocalId(localId);
    return appGuid;
  }
}
