package net.bytle.tower.eraldy.module.mailing.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.module.mailing.model.MailingItemGuid;
import net.bytle.vertx.guid.GuidDeSer;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;


public class JacksonMailingItemGuidDeserializer extends JacksonJsonStringDeserializer<MailingItemGuid> {

  private final GuidDeSer guidDeSer;

  public JacksonMailingItemGuidDeserializer(GuidDeSer guidDeSer) {
    this.guidDeSer = guidDeSer;

  }

  @Override
  public MailingItemGuid deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
    try {
      return deserialize(value);
    } catch (CastException e) {
      throw new IOException("The list guid value (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

  }


  @Override
  public MailingItemGuid deserialize(String value) throws CastException {
    long[] ids;
    try {
      ids = this.guidDeSer.deserialize(value);
    } catch (CastException e) {
      throw new CastException("The mailing guid (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

    MailingItemGuid mailingItemGuid = new MailingItemGuid();
    mailingItemGuid.setRealmId(ids[0]);
    mailingItemGuid.setMailingId(ids[1]);
    mailingItemGuid.setUserId(ids[2]);
    mailingItemGuid.setHash(value);
    return mailingItemGuid;
  }
}
