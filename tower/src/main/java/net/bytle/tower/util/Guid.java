package net.bytle.tower.util;

import io.vertx.core.Vertx;
import io.vertx.json.schema.ValidationException;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.model.openapi.Realm;

/**
 * A wrapper around the combo GUID
 */
public class Guid {

  public final static String GUID = "guid";
  public static final String GUID_SEPARATOR = "-";

  /**
   * The hash has a realm id and an object id
   */
  private static final String REALM_ID_OBJECT_ID_TYPE = "realm_id_object_id";
  private static final String REALM_ID_TWO_OBJECT_ID_TYPE = "realm_id_two_object_id";
  /**
   * The case of realm id
   */
  private static final String REALM_ID_TYPE = "one_object_id";
  private long realmId;
  private long firstObjectId;
  private long secondObjectId;

  public static long[] getIdsFromGuid(String hashPrefix, String guid, Vertx vertx) throws CastException {
    if (!guid.startsWith(hashPrefix)) {
      throw new CastException("The guid (" + guid + ") is not valid, it does not start with " + hashPrefix);
    }
    String hashId = guid.substring(hashPrefix.length() + GUID_SEPARATOR.length());
    long[] ids = HashId.get(vertx).decode(hashId);
    if (!(ids.length == 2 // other ids
      || ids.length == 1 // realm
    )) {
      throw new CastException("The guid is not conform."); //  should have two ids
    }
    return ids;
  }

  /**
   * @param guid         a guid
   * @param requestRealm the realm that is
   * @param vertx        the vertx
   * @return the second id
   * @throws CastException if this is not a guid
   *                       Note we throw because guid is not the only identifiant that we may get
   *                       We may also get an email
   */
  public static long getIdFromGuidAndRealm(String guid, Realm requestRealm, Vertx vertx) throws CastException {
    long[] ids;
    ids = getIdsFromGuid("unknown", guid, vertx);
    long realmId = ids[0];
    if (
      requestRealm != null // case when we create an app, there is no request realm
        && realmId != requestRealm.getLocalId()
    ) {
      throw ValidationException.create("The guid realm is not the same than the request realm", "guid", guid);
    }
    return ids[1];
  }

  public static long getSingleIdFromGuid(String hashPrefix, String guid, Vertx vertx) throws CastException {
    long[] ids;
    ids = getIdsFromGuid(hashPrefix, guid, vertx);
    return ids[0];
  }

  public static String getGuid(Long id, Realm realm, Vertx vertx) {
    return HashId.get(vertx).encode(realm.getLocalId(), id);
  }

  public static String getGuid(String HashPrefix, Realm realm, Long id, Vertx vertx) {
    return HashPrefix + GUID_SEPARATOR + HashId.get(vertx).encode(realm.getLocalId(), id);
  }

  public static String getGuid(Long id1, Long id2, Vertx vertx) {
    return HashId.get(vertx).encode(id1, id2);
  }

  public static String getGuid(Long id, Vertx vertx) {
    return HashId.get(vertx).encode(id);
  }


  public static Guid createObjectFromRealmIdAndOneObjectId(String shortPrefix, String userGuid, Vertx vertx) throws CastException {
    return new config(userGuid, vertx)
      .setHashPrefix(shortPrefix)
      .setType(REALM_ID_OBJECT_ID_TYPE)
      .build();
  }

  public static String createGuidStringFromRealmAndObjectId(String shortPrefix, Realm realm, Long id, Vertx vertx) {
    String hashGuid = Guid.getGuid(realm.getLocalId(), id, vertx);
    return shortPrefix + GUID_SEPARATOR + hashGuid;
  }

  public static String createGuidStringFromObjectId(String suffix, Long id, Vertx vertx) {
    return suffix + GUID_SEPARATOR + HashId.get(vertx).encode(id);
  }

  public static Guid createObjectFromRealmIdAndTwoObjectId(String shortPrefix, String guid, Vertx vertx) throws CastException {
    return new config(guid, vertx)
      .setHashPrefix(shortPrefix)
      .setType(REALM_ID_TWO_OBJECT_ID_TYPE)
      .build();
  }

  public static String createGuidStringFromRealmAndTwoObjectId(String shortPrefix, Realm realm, Long id1, Long id2, Vertx vertx) {
    String hashGuid = HashId.get(vertx).encode(realm.getLocalId(), id1, id2);
    return shortPrefix + GUID_SEPARATOR + hashGuid;
  }

  public static Guid createObjectFromRealmId(String shortPrefix, String guid, Vertx vertx) throws CastException {
    return new config(guid, vertx)
      .setHashPrefix(shortPrefix)
      .setType(REALM_ID_TYPE)
      .build();
  }

  public long getRealmId() {
    return this.realmId;
  }

  public long getFirstObjectId() {
    return this.firstObjectId;
  }

  public long getSecondObjectId() {
    return this.secondObjectId;
  }

  private static class config {
    private final String guid;
    private final Vertx vertx;
    private String hashPrefix;
    private String type;

    public config(String userGuid, Vertx vertx) {
      this.guid = userGuid;
      this.vertx = vertx;
    }

    public config setHashPrefix(String shortPrefix) {
      this.hashPrefix = shortPrefix;
      return this;
    }

    public config setType(String type) {
      this.type = type;
      return this;
    }

    public Guid build() throws CastException {

      Guid guid = new Guid();
      if (!this.guid.startsWith(hashPrefix)) {
        throw new CastException("The guid is not valid, it does not start with " + hashPrefix);
      }
      String hashId = this.guid.substring(hashPrefix.length() + GUID_SEPARATOR.length());
      long[] ids = HashId.get(vertx).decode(hashId);
      switch (this.type) {
        case REALM_ID_TYPE:
          this.checkIds(ids, 1);
          guid.realmId = ids[0];
          break;
        case REALM_ID_OBJECT_ID_TYPE:
          this.checkIds(ids, 2);
          guid.realmId = ids[0];
          guid.firstObjectId = ids[1];
          break;
        case REALM_ID_TWO_OBJECT_ID_TYPE:
          this.checkIds(ids, 3);
          guid.realmId = ids[0];
          guid.firstObjectId = ids[1];
          guid.secondObjectId = ids[2];
          break;
        default:
          throw new InternalException("The Guid type " + this.type + " is not in the branch");
      }


      return guid;
    }

    private void checkIds(long[] ids, int expectedIdCount) throws CastException {
      if (ids.length != expectedIdCount) {
        throw new CastException("The guid is not conform because it has " + ids.length + " id and not " + expectedIdCount);
      }
    }

  }
}
