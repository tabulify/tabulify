package net.bytle.tower.eraldy.module.mailing.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.tower.eraldy.module.mailing.model.MailingJobGuid;
import net.bytle.vertx.guid.GuidDeSer;
import net.bytle.vertx.jackson.JacksonJsonStringSerializer;

import java.io.IOException;

public class JacksonMailingJobGuidSerializer extends JacksonJsonStringSerializer<MailingJobGuid> {



  private final GuidDeSer guidDeSer;
  public JacksonMailingJobGuidSerializer(GuidDeSer guidDeSer) {
    this.guidDeSer = guidDeSer;

  }

  @Override
  public void serialize(MailingJobGuid value, JsonGenerator gen, SerializerProvider serializers) throws IOException {


    gen.writeString(serialize(value));

  }

  @Override
  public String serialize(MailingJobGuid value) {
    return guidDeSer.serialize(value.getRealmId(),value.getLocalId());

  }
}
