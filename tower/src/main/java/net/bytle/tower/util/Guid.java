package net.bytle.tower.util;

import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.module.realm.model.Realm;
import net.bytle.vertx.HashId;

import java.util.ArrayList;
import java.util.List;

/**
 * A wrapper around the combo GUID
 */
public class Guid {

  public final static String GUID = "guid";
  public static final String GUID_SEPARATOR = "-";

  /**
   * The hash has a realm id and an object id
   */
  public static final String REALM_ID_OBJECT_ID_TYPE = "realm_id_object_id";
  public static final String REALM_ID_TWO_OBJECT_ID_TYPE = "realm_id_two_object_id";
  /**
   * The case of realm id
   */
  public static final String ONE_ID_TYPE = "one_object_id";
  private final builder builder;

  public Guid(builder builder) {
    this.builder = builder;
  }

  public static builder builder(HashId hashIds, String hashPrefix) {
    return new builder(hashIds, hashPrefix);
  }


  @Override
  public String toString() {
    List<Long> ids = new ArrayList<>();
    ids.add(this.builder.organizationOrRealmId);
    if (this.builder.firstObjectId != null) {
      ids.add(this.builder.firstObjectId);
    }
    if (this.builder.secondObjectId != null) {
      ids.add(this.builder.secondObjectId);
    }
    long[] arrayIds = new long[ids.size()];
    for (int i = 0; i < ids.size(); i++) {
      arrayIds[i] = ids.get(i);
    }
    String hashGuid = this.builder.hashIds.encode(arrayIds);
    return this.builder.guidPrefix + GUID_SEPARATOR + hashGuid;
  }

  public long getRealmOrOrganizationId() {
    return this.builder.organizationOrRealmId;
  }

  public long validateRealmAndGetFirstObjectId(Long realmId) {
    if (!this.builder.organizationOrRealmId.equals(realmId)) {
      throw new InternalException("The expected realm id (" + realmId + ") is not the same than the actual (" + this.builder.organizationOrRealmId + ")");
    }
    return this.builder.firstObjectId;
  }

  public long validateAndGetSecondObjectId(Long realmId) {
    if (!this.builder.organizationOrRealmId.equals(realmId)) {
      throw new InternalException("The expected realm id (" + realmId + ") is not the same than the actual (" + this.builder.organizationOrRealmId + ")");
    }
    return this.builder.secondObjectId;
  }


  public static class builder {

    private final HashId hashIds;
    private final String guidPrefix;

    private Long organizationOrRealmId;
    private Long firstObjectId;
    private Long secondObjectId;


    public builder(HashId hashId, String guidPrefix) {
      this.hashIds = hashId;
      this.guidPrefix = guidPrefix;
    }


    public builder setCipherText(String cipherText, String type) throws CastException {

      if (!cipherText.startsWith(this.guidPrefix)) {
        throw new CastException("The guid is not valid, it does not start with " + this.guidPrefix);
      }
      String hashId = cipherText.substring(this.guidPrefix.length() + GUID_SEPARATOR.length());
      long[] ids = this.hashIds.decode(hashId);
      switch (type) {
        case ONE_ID_TYPE:
          this.checkIds(ids, 1);
          this.organizationOrRealmId = ids[0];
          break;
        case REALM_ID_OBJECT_ID_TYPE:
          this.checkIds(ids, 2);
          this.organizationOrRealmId = ids[0];
          this.firstObjectId = ids[1];
          break;
        case REALM_ID_TWO_OBJECT_ID_TYPE:
          this.checkIds(ids, 3);
          this.organizationOrRealmId = ids[0];
          this.firstObjectId = ids[1];
          this.secondObjectId = ids[2];
          break;
        default:
          throw new InternalException("The Guid type " + type + " is not in the branch");
      }
      return this;
    }


    private void checkIds(long[] ids, int expectedIdCount) throws CastException {
      if (ids.length != expectedIdCount) {
        throw new CastException("The guid is not conform because it has " + ids.length + " id and not " + expectedIdCount);
      }
    }

    public builder setRealm(Realm realm) {
      this.organizationOrRealmId = realm.getGuid().getLocalId();
      return this;
    }

    public Guid build() {
      if (this.organizationOrRealmId == null) {
        throw new InternalException("The realm or organization id should not be null");
      }
      return new Guid(this);
    }

    public builder setOrganizationOrRealmId(Long id) {
      this.organizationOrRealmId = id;
      return this;
    }

    public builder setFirstObjectId(Long id1) {
      this.firstObjectId = id1;
      return this;
    }

    public builder setSecondObjectId(Long id2) {
      this.secondObjectId = id2;
      return this;
    }
  }


}
