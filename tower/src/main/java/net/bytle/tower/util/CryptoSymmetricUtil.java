package net.bytle.tower.util;


import io.vertx.core.Vertx;
import net.bytle.crypto.CryptoSymmetricCipher;
import net.bytle.exception.InternalException;
import net.bytle.exception.NoSecretException;
import net.bytle.vertx.ConfigAccessor;

import java.util.HashMap;
import java.util.Map;


public class CryptoSymmetricUtil {
  private static final String SECRET_SYMMETRIC_DATA_ENCRYPTION_CONF = "secret.symmetric.data.encryption";

  private static final Map<Vertx, CryptoSymmetricUtil> cryptoSymmetricMap = new HashMap<>();
  private final String secret;
  private final CryptoSymmetricCipher aes;

  public CryptoSymmetricUtil(String secret) {
    this.secret = secret;
    this.aes = CryptoSymmetricCipher.AES_CBC_PKCS5PADDING;
  }

  public static CryptoSymmetricUtil get(Vertx vertx) {
    CryptoSymmetricUtil cryptoSymmetric = cryptoSymmetricMap.get(vertx);
    if (cryptoSymmetric == null) {
      throw new InternalException("Crypto Symmetric should not be null");
    }
    return cryptoSymmetric;
  }

  public String encrypt(String plainText) {
    return aes.encrypt(this.secret, plainText);
  }

  public String decrypt(String cipherText) {
    return aes.decrypt(this.secret, cipherText);
  }


  public static Config config(Vertx vertx, ConfigAccessor jsonConfig) {
    return new Config(vertx)
      .setSecret((String) jsonConfig.getValue(SECRET_SYMMETRIC_DATA_ENCRYPTION_CONF));
  }

  public static class Config {
    private final Vertx vertx;
    private String secret;


    public Config(Vertx vertx) {
      this.vertx = vertx;
    }

    public Config setSecret(String secret) {
      this.secret = secret;
      return this;
    }

    public CryptoSymmetricUtil create() throws NoSecretException {

      if (this.secret == null) {
        throw new NoSecretException("A symmetric secret is mandatory. You can set set in the conf with the attribute (" + SECRET_SYMMETRIC_DATA_ENCRYPTION_CONF + ")");
      }

      CryptoSymmetricUtil cryptoSymmetric = new CryptoSymmetricUtil(this.secret);
      cryptoSymmetricMap.put(vertx, cryptoSymmetric);
      return cryptoSymmetric;
    }
  }
}
