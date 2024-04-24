package net.bytle.tower.eraldy.module.organization.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.module.organization.db.OrganizationProvider;
import net.bytle.tower.eraldy.module.organization.model.OrgaGuid;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.HashId;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;

import static net.bytle.tower.util.Guid.ONE_ID_TYPE;

public class JacksonOrgaGuidDeserializer extends JacksonJsonStringDeserializer<OrgaGuid> {

  private final HashId hashIds;

  public JacksonOrgaGuidDeserializer(EraldyApiApp apiApp) {
    this.hashIds = apiApp.getHttpServer().getServer().getHashId();
  }

  @Override
  public OrgaGuid deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
      try {
          return this.deserialize(value);
      } catch (CastException e) {
          throw new RuntimeException(e.getMessage(),e);
      }

  }

  public OrgaGuid deserialize(String value) throws CastException {
    Guid orgaGuid;
    try {
      orgaGuid = new Guid.builder(this.hashIds, OrganizationProvider.GUID_PREFIX)
        .setCipherText(value, ONE_ID_TYPE)
        .build();
    } catch (CastException e) {
      throw new CastException("The orga guid (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

    OrgaGuid orgaUserGuid = new OrgaGuid();
    orgaUserGuid.setLocalId(orgaGuid.getRealmOrOrganizationId());

    return orgaUserGuid;
  }

}
