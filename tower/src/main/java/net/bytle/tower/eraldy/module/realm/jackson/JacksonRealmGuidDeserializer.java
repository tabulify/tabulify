package net.bytle.tower.eraldy.module.realm.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.module.realm.db.RealmProvider;
import net.bytle.tower.eraldy.module.realm.model.RealmGuid;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.HashId;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;

import static net.bytle.tower.util.Guid.ONE_ID_TYPE;

public class JacksonRealmGuidDeserializer extends JacksonJsonStringDeserializer<RealmGuid> {

  private final HashId hashIds;

  public JacksonRealmGuidDeserializer(EraldyApiApp apiApp) {
    this.hashIds = apiApp.getHttpServer().getServer().getHashId();
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
    Guid guid;
    try {
      guid = new Guid.builder(this.hashIds, RealmProvider.REALM_GUID_PREFIX)
        .setCipherText(value, ONE_ID_TYPE)
        .build();
    } catch (CastException e) {
      throw new CastException("The realm guid (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

    return new RealmGuid(guid.getRealmOrOrganizationId());
  }

}
