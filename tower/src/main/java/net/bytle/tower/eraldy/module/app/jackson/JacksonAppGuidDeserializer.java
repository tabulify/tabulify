package net.bytle.tower.eraldy.module.app.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.module.app.model.AppGuid;
import net.bytle.tower.eraldy.objectProvider.AppProvider;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.HashId;

import java.io.IOException;

import static net.bytle.tower.util.Guid.REALM_ID_OBJECT_ID_TYPE;

public class JacksonAppGuidDeserializer extends JsonDeserializer<AppGuid> {

  private final HashId hashIds;

  public JacksonAppGuidDeserializer(EraldyApiApp apiApp) {
    this.hashIds = apiApp.getHttpServer().getServer().getHashId();

  }

  @Override
  public AppGuid deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
    Guid userGuidObject;
    try {
      userGuidObject = new Guid.builder(this.hashIds, AppProvider.APP_GUID_PREFIX)
        .setCipherText(value, REALM_ID_OBJECT_ID_TYPE)
        .build();
    } catch (CastException e) {
      throw new IOException("The app guid (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }


    AppGuid appGuid = new AppGuid();
    long realmId = userGuidObject.getRealmOrOrganizationId();
    appGuid.setRealmId(realmId);
    long localId = userGuidObject.validateRealmAndGetFirstObjectId(realmId);
    appGuid.setLocalId(localId);
    return appGuid;

  }


}
