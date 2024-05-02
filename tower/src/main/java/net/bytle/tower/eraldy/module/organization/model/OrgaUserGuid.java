package net.bytle.tower.eraldy.module.organization.model;

import net.bytle.exception.InternalException;
import net.bytle.tower.EraldyModel;
import net.bytle.tower.eraldy.module.user.model.UserGuid;

import java.util.Objects;

/**
 * An orga user guid is:
 * * a user guid where the realm is the eraldy realm 1
 * * and an organization
 * <p>
 * At first, we were extending the UserGuid, but it comes with multiple problem
 * when serializing as the serializer may choose the UserGuid serializer
 * and not the OrgaUserGuid
 * They are 2 differents id
 * By not extending, you can't check the equality (they are the same user)
 */
public class OrgaUserGuid extends UserGuid {


  /**
   * The database id
   */
  private long organizationId;
  /**
   * The hash public representation
   */
  private String publicHash;

  public OrgaUserGuid() {
    super();
    setRealmId(EraldyModel.REALM_LOCAL_ID);
  }


  @Override
  public void setRealmId(Long localId) {

    if (!Objects.equals(localId, EraldyModel.REALM_LOCAL_ID)) {
      throw new InternalException("An orga user should have the Eraldy Realm");
    }
    super.setRealmId(localId);

  }

  public void setOrganizationId(Long organizationId) {

    this.organizationId = organizationId;

  }

  @Override
  public long getRealmId() {

    return EraldyModel.REALM_LOCAL_ID;

  }

  public long getOrganizationId() {

    return this.organizationId;

  }


  public void setPublicHash(String publicHash) {
    this.publicHash = publicHash;
  }

  public OrgaGuid toOrgaGuid() {
    return new OrgaGuid(this.organizationId);
  }

  @SuppressWarnings("unused")
  public String getPublicHash() {
    return publicHash;
  }
}
