package net.bytle.tower.eraldy.module.organization.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.tower.eraldy.module.user.db.UserProvider;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.HashId;

import java.io.IOException;

import static net.bytle.tower.util.Guid.REALM_ID_OBJECT_ID_TYPE;

public class JacksonOrgaUserGuidDeserializer extends JsonDeserializer<OrgaUserGuid> {

  private final HashId hashIds;
  private final Long eraldyRealmId;

  public JacksonOrgaUserGuidDeserializer(EraldyApiApp apiApp) {
    this.hashIds = apiApp.getHttpServer().getServer().getHashId();
    this.eraldyRealmId = apiApp.getEraldyModel().getRealmLocalId();
  }

  @Override
  public OrgaUserGuid deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
    Guid userGuid;
    try {
      userGuid = new Guid.builder(this.hashIds, UserProvider.USR_GUID_PREFIX)
        .setCipherText(value, REALM_ID_OBJECT_ID_TYPE)
        .build();
    } catch (CastException e) {
      throw new IOException("The user guid (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }
    long realmId = userGuid.getRealmOrOrganizationId();
    if (realmId != this.eraldyRealmId) {
      throw new IOException("The user guid (" + value + ") is not a organization guid.");
    }
    OrgaUserGuid orgaUserGuid = new OrgaUserGuid();
    orgaUserGuid.setLocalId(userGuid.validateRealmAndGetFirstObjectId(realmId));
    return orgaUserGuid;

  }

}
