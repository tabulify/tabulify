package net.bytle.tower.eraldy.module.organization.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.tower.EraldyModel;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.tower.eraldy.module.user.db.UserProvider;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.HashId;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;

import static net.bytle.tower.util.Guid.REALM_ID_OBJECT_ID_TYPE;

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
      userGuid = new Guid.builder(this.hashIds, UserProvider.USR_GUID_PREFIX)
        .setCipherText(value, REALM_ID_OBJECT_ID_TYPE)
        .build();
    } catch (CastException e) {
      throw new CastException("The user guid (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }
    long realmId = userGuid.getRealmOrOrganizationId();
    if (realmId != EraldyModel.REALM_LOCAL_ID) {
      throw new CastException("The user guid (" + value + ") is not a organization guid.");
    }
    OrgaUserGuid orgaUserGuid = new OrgaUserGuid();
    orgaUserGuid.setLocalId(userGuid.validateRealmAndGetFirstObjectId(realmId));
    orgaUserGuid.setRealmId(EraldyModel.REALM_LOCAL_ID);
    return orgaUserGuid;
  }

}
