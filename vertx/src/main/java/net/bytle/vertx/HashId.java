package net.bytle.vertx;

import net.bytle.exception.IllegalConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hashids.Hashids;

/**
 * Create Hash from integers and back
 */
public class HashId {

  static Logger LOGGER = LogManager.getLogger(HashId.class);
  private final Hashids hashIds;
  private String secret;
  public static final String SECRET_ID_CONF = "secret.id";


  HashId(ConfigAccessor jsonConfig) throws IllegalConfiguration {
    this.secret = jsonConfig.getString(SECRET_ID_CONF);
    if (secret == null) {
      throw new IllegalConfiguration("The conf (" + SECRET_ID_CONF + ") is mandatory for HashId");
    }
    /**
     * alphabet is base62. See {@link Hashids#DEFAULT_ALPHABET}
     */
    int minimumLength = 7;
    this.hashIds = new Hashids(this.secret, minimumLength);

    LOGGER.info("HashId enabled and initialized with the secret conf (" + SECRET_ID_CONF + ")");

  }


  public long[] decode(String cipherText) {
    return hashIds.decode(cipherText);
  }

  public String encode(long... numbers) {
    return hashIds.encode(numbers);
  }


}
