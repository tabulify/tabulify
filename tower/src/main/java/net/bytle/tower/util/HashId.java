package net.bytle.tower.util;

import io.vertx.core.Vertx;
import net.bytle.exception.NoSecretException;
import net.bytle.vertx.ConfigAccessor;
import org.hashids.Hashids;

import java.util.HashMap;
import java.util.Map;

public class HashId {


  private static final Map<Vertx, HashId> hashIds = new HashMap<>();
  public static final String SECRET_ID_CONF = "secret.id";
  private final Hashids hashId;


  private HashId(Hashids hashId) {
    this.hashId = hashId;
  }


  public static HashId get(Vertx vertx) {
    return hashIds.get(vertx);
  }


  public static config config(Vertx vertx, ConfigAccessor jsonConfig) {
    return new config(vertx)
      .setSecret((String) jsonConfig.getValue(SECRET_ID_CONF));
  }

  public long[] decode(String cipherText) {
    return hashId.decode(cipherText);
  }

  public String encode(long... numbers) {
    return hashId.encode(numbers);
  }

  public static class config {
    private final Vertx vertx;
    private String secret;

    public config(Vertx vertx) {
      this.vertx = vertx;
    }

    config setSecret(String secret) {
      this.secret = secret;
      return this;
    }

    public HashId create() throws NoSecretException {

      /**
       * Built hash ids
       */
      if (this.secret == null) {
        throw new NoSecretException("A secret is mandatory to encrypt id. You can set set in the conf with the attribute (" + SECRET_ID_CONF + ")");
      }
      /**
       * alphabet is base62. See {@link Hashids#DEFAULT_ALPHABET}
       */
      int minimumLength = 7;
      Hashids hashIds = new Hashids(this.secret, minimumLength);

      /**
       * Wrapper with vertx
       */
      HashId hashId = new HashId(hashIds);
      HashId.hashIds.put(vertx, hashId);
      return hashId;
    }
  }
}
