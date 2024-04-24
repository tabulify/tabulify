package net.bytle.tower.eraldy.module.organization.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.module.organization.model.OrgaGuid;
import net.bytle.tower.eraldy.objectProvider.OrganizationProvider;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.HashId;
import net.bytle.vertx.jackson.JacksonJsonStringSerializer;

import java.io.IOException;

public class JacksonOrgaGuidSerializer extends JacksonJsonStringSerializer<OrgaGuid> {



  private final HashId hashIds;
  public JacksonOrgaGuidSerializer(EraldyApiApp apiApp) {
    this.hashIds = apiApp.getHttpServer().getServer().getHashId();

  }

  @Override
  public void serialize(OrgaGuid value, JsonGenerator gen, SerializerProvider serializers) throws IOException {


    gen.writeString(serialize(value));

  }

  @Override
  public String serialize(OrgaGuid value) {
    return Guid.builder(this.hashIds, OrganizationProvider.GUID_PREFIX)
      .setOrganizationOrRealmId(value.getLocalId())
      .build()
      .toString();
  }
}
