package net.bytle.tower.eraldy.module.mailing.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.module.mailing.model.MailingGuid;
import net.bytle.vertx.guid.GuidDeSer;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;


public class JacksonMailingGuidDeserializer extends JacksonJsonStringDeserializer<MailingGuid> {

  private final GuidDeSer guidDeSer;

  public JacksonMailingGuidDeserializer(GuidDeSer guidDeSer) {
    this.guidDeSer = guidDeSer;

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
    long[] ids;
    try {
      ids = this.guidDeSer.deserialize(value);
    } catch (CastException e) {
      throw new CastException("The mailing guid (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

    MailingGuid mailingGuid = new MailingGuid();
    long realmId = ids[0];
    mailingGuid.setRealmId(realmId);
    long localId = ids[1];
    mailingGuid.setLocalId(localId);
    mailingGuid.setHash(value);
    return mailingGuid;
  }
}
