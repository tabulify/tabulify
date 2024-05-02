package net.bytle.tower.eraldy.module.mailing.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.module.mailing.model.MailingJobGuid;
import net.bytle.vertx.guid.GuidDeSer;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;


public class JacksonMailingJobGuidDeserializer extends JacksonJsonStringDeserializer<MailingJobGuid> {

  private final GuidDeSer guidDeSer;

  public JacksonMailingJobGuidDeserializer(GuidDeSer guidDeSer) {
    this.guidDeSer = guidDeSer;

  }

  @Override
  public MailingJobGuid deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
    try {
      return deserialize(value);
    } catch (CastException e) {
      throw new IOException("The mailing job guid value (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

  }


  @Override
  public MailingJobGuid deserialize(String value) throws CastException {
    long[] ids;
    try {
      ids = this.guidDeSer.deserialize(value);
    } catch (CastException e) {
      throw new CastException("The mailing job guid (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

    MailingJobGuid mailingGuid = new MailingJobGuid();
    long realmId = ids[0];
    mailingGuid.setRealmId(realmId);
    long localId = ids[1];
    mailingGuid.setLocalId(localId);
    return mailingGuid;
  }
}
