package net.bytle.tower.eraldy.module.mailing.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.tower.eraldy.module.mailing.model.MailingItemGuid;
import net.bytle.vertx.guid.GuidDeSer;
import net.bytle.vertx.jackson.JacksonJsonStringSerializer;

import java.io.IOException;

public class JacksonMailingItemGuidSerializer extends JacksonJsonStringSerializer<MailingItemGuid> {



  private final GuidDeSer guidDeSer;
  public JacksonMailingItemGuidSerializer(GuidDeSer guidDeSer) {
    this.guidDeSer = guidDeSer;

  }

  @Override
  public void serialize(MailingItemGuid value, JsonGenerator gen, SerializerProvider serializers) throws IOException {


    gen.writeString(serialize(value));

  }

  @Override
  public String serialize(MailingItemGuid value) {
    return guidDeSer.serialize(value.getRealmId(),value.getMailingId(), value.getUserId());

  }
}
