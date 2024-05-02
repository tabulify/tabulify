package net.bytle.tower.eraldy.module.mailing.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.module.mailing.db.mailing.MailingProvider;
import net.bytle.tower.eraldy.module.mailing.model.MailingGuid;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.HashId;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;

import static net.bytle.tower.util.Guid.REALM_ID_OBJECT_ID_TYPE;

public class JacksonMailingGuidDeserializer extends JacksonJsonStringDeserializer<MailingGuid> {

  private final HashId hashIds;

  public JacksonMailingGuidDeserializer(EraldyApiApp apiApp) {
    this.hashIds = apiApp.getHttpServer().getServer().getHashId();

  }

  @Override
  public MailingGuid deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
    try {
      return deserialize(value);
    } catch (CastException e) {
      throw new IOException("The list guid value (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

  }


  @Override
  public MailingGuid deserialize(String value) throws CastException {
    Guid userGuidObject;
    try {
      userGuidObject = new Guid.builder(this.hashIds, MailingProvider.MAILING_GUID_PREFIX)
        .setCipherText(value, REALM_ID_OBJECT_ID_TYPE)
        .build();
    } catch (CastException e) {
      throw new CastException("The mailing guid (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

    MailingGuid mailingGuid = new MailingGuid();
    long realmId = userGuidObject.getRealmOrOrganizationId();
    mailingGuid.setRealmId(realmId);
    long localId = userGuidObject.validateRealmAndGetFirstObjectId(realmId);
    mailingGuid.setLocalId(localId);
    mailingGuid.setPublicHash(value);
    return mailingGuid;
  }
}
