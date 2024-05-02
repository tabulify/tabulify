package net.bytle.tower.eraldy.module.mailing.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.module.mailing.db.mailing.MailingProvider;
import net.bytle.tower.eraldy.module.mailing.model.MailingGuid;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.HashId;
import net.bytle.vertx.jackson.JacksonJsonStringSerializer;

import java.io.IOException;

public class JacksonMailingGuidSerializer extends JacksonJsonStringSerializer<MailingGuid> {



  private final HashId hashIds;
  public JacksonMailingGuidSerializer(EraldyApiApp apiApp) {
    this.hashIds = apiApp.getHttpServer().getServer().getHashId();

  }

  @Override
  public void serialize(MailingGuid value, JsonGenerator gen, SerializerProvider serializers) throws IOException {


    gen.writeString(serialize(value));

  }

  @Override
  public String serialize(MailingGuid value) {
    return Guid.builder(this.hashIds, MailingProvider.MAILING_GUID_PREFIX)
      .setOrganizationOrRealmId(value.getRealmId())
      .setFirstObjectId(value.getLocalId())
      .build()
      .toString();
  }
}
