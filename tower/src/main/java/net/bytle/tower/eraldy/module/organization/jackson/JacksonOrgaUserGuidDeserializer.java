package net.bytle.tower.eraldy.module.organization.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.tower.EraldyModel;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.module.organization.db.OrganizationUserProvider;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.HashId;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;

import static net.bytle.tower.util.Guid.REALM_ID_TWO_OBJECT_ID_TYPE;

public class JacksonOrgaUserGuidDeserializer extends JacksonJsonStringDeserializer<OrgaUserGuid> {

  private final HashId hashIds;

  public JacksonOrgaUserGuidDeserializer(EraldyApiApp apiApp) {
    this.hashIds = apiApp.getHttpServer().getServer().getHashId();
  }

  @Override
  public OrgaUserGuid deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
      try {
          return this.deserialize(value);
      } catch (CastException e) {
          throw new RuntimeException(e.getMessage(),e);
      }

  }

  public OrgaUserGuid deserialize(String value) throws CastException {
    Guid userGuid;
    try {
      userGuid = new Guid.builder(this.hashIds, OrganizationUserProvider.GUID_PREFIX)
        .setCipherText(value, REALM_ID_TWO_OBJECT_ID_TYPE)
        .build();
    } catch (CastException e) {
      throw new CastException("The user guid (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }
    long realmId = userGuid.getRealmOrOrganizationId();
    if (realmId != EraldyModel.REALM_LOCAL_ID) {
      throw new CastException("The user guid (" + value + ") is not a organization guid because the realm is not eraldy.");
    }
    OrgaUserGuid orgaUserGuid = new OrgaUserGuid();
    orgaUserGuid.setLocalId(userGuid.validateRealmAndGetFirstObjectId(realmId));
    orgaUserGuid.setRealmId(EraldyModel.REALM_LOCAL_ID);
    orgaUserGuid.setOrganizationId(userGuid.validateAndGetSecondObjectId(realmId));
    return orgaUserGuid;
  }

}
