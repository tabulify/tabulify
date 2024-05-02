package net.bytle.vertx.guid;

import net.bytle.exception.InternalException;
import net.bytle.type.Arrayss;
import net.bytle.vertx.ConfigAccessor;
import net.bytle.vertx.ConfigIllegalException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hashids.Hashids;

import java.util.HashMap;
import java.util.Map;

/**
 * Create Hash from integers and back
 */
public class HashId {

  static Logger LOGGER = LogManager.getLogger(HashId.class);
  private final Hashids hashIds;
  public static final String SECRET_ID_CONF = "secret.id";
  private final Map<String, GuidDeSer> guids = new HashMap<>();


  public HashId(ConfigAccessor jsonConfig) throws ConfigIllegalException {
    String secret = jsonConfig.getString(SECRET_ID_CONF);
    if (secret == null) {
      throw new ConfigIllegalException("The conf (" + SECRET_ID_CONF + ") is mandatory for HashId");
    }
    /**
     * alphabet is base62. See {@link Hashids#DEFAULT_ALPHABET}
     */
    int minimumLength = 7;
    this.hashIds = new Hashids(secret, minimumLength);

    LOGGER.info("HashId enabled and initialized with the secret conf (" + SECRET_ID_CONF + ")");

  }


  public long[] decode(String cipherText) {
    return hashIds.decode(cipherText);
  }

  public String encode(long number, long... numbers) {
    return hashIds.encode(Arrayss.concat(number, numbers));
  }


  public GuidDeSer getGuidDeSer(String guidPrefix, int idsCount) {
    String guidPrefixNormalized = guidPrefix = guidPrefix.toLowerCase();
    if (this.guids.containsKey(guidPrefixNormalized)) {
      throw new InternalException("The guid prefix (" + guidPrefixNormalized + ") was already registered and cannot be used twice");
    }
    GuidDeSer guid = new GuidDeSer(this, guidPrefix, idsCount);
    this.guids.put(guidPrefixNormalized,guid);
    return guid;
  }
}
