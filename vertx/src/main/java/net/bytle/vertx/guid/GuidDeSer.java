package net.bytle.vertx.guid;

import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;

/**
 * A GUID serializer, deserializer
 */
public class GuidDeSer {

  public final static String GUID = "guid";
  public static final String GUID_SEPARATOR = "-";

  private final int numberOfId;
  private final String guidPrefix;
  private final HashId hashIds;

  public GuidDeSer(HashId hashIds, String guidPrefix, int numberOfId) {

    this.hashIds = hashIds;
    this.guidPrefix = guidPrefix;
    this.numberOfId = numberOfId;

  }

  public String serialize(long firstId, long... otherIds) {

    int actualIdCount = 1 + otherIds.length;
    if (this.numberOfId != actualIdCount) {
      throw new InternalException("The number of passed id should be " + this.numberOfId + ", not " + actualIdCount);
    }
    String hashGuid = this.hashIds.encode(firstId, otherIds);
    return this.guidPrefix + GUID_SEPARATOR + hashGuid;

  }

  public long[] deserialize(String cipherText) throws CastException {

    if (!cipherText.startsWith(this.guidPrefix)) {
      throw new CastException("The guid is not valid, it does not start with " + this.guidPrefix);
    }
    String hashId = cipherText.substring(this.guidPrefix.length() + GUID_SEPARATOR.length());
    long[] ids = this.hashIds.decode(hashId);
    this.checkIds(ids, this.numberOfId);
    return ids;
  }

  private void checkIds(long[] ids, int expectedIdCount) throws CastException {
    if (ids.length != expectedIdCount) {
      throw new CastException("The guid is not conform because it has " + ids.length + " id and not " + expectedIdCount);
    }
  }


}
