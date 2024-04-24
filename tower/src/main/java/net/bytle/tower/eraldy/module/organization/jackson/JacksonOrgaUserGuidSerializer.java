package net.bytle.tower.eraldy.module.organization.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.tower.EraldyModel;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.module.organization.db.OrganizationUserProvider;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.HashId;
import net.bytle.vertx.jackson.JacksonJsonStringSerializer;

import java.io.IOException;

public class JacksonOrgaUserGuidSerializer extends JacksonJsonStringSerializer<OrgaUserGuid> {



  private final HashId hashIds;
  public JacksonOrgaUserGuidSerializer(EraldyApiApp apiApp) {
    this.hashIds = apiApp.getHttpServer().getServer().getHashId();

  }

  @Override
  public void serialize(OrgaUserGuid value, JsonGenerator gen, SerializerProvider serializers) throws IOException {


    gen.writeString(serialize(value));

  }

  @Override
  public String serialize(OrgaUserGuid value) {
    return Guid.builder(this.hashIds, OrganizationUserProvider.GUID_PREFIX)
      .setOrganizationOrRealmId(EraldyModel.REALM_LOCAL_ID)
      .setFirstObjectId(value.getLocalId())
      .setSecondObjectId(value.getOrganizationId())
      .build()
      .toString();
  }
}
