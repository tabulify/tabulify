package net.bytle.tower.eraldy.module.organization.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.tower.EraldyModel;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.tower.eraldy.module.user.db.UserProvider;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.HashId;

import java.io.IOException;

public class JacksonOrgaUserGuidSerializer extends JsonSerializer<OrgaUserGuid> {



  private final HashId hashIds;
  public JacksonOrgaUserGuidSerializer(EraldyApiApp apiApp) {
    this.hashIds = apiApp.getHttpServer().getServer().getHashId();

  }

  @Override
  public void serialize(OrgaUserGuid value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

    String guidHash = Guid.builder(this.hashIds, UserProvider.USR_GUID_PREFIX)
      .setOrganizationOrRealmId(EraldyModel.REALM_LOCAL_ID)
      .setFirstObjectId(value.getLocalId())
      .build()
      .toString();

    gen.writeString(guidHash);

  }

}
