package net.bytle.vertx;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.impl.jose.JWK;
import io.vertx.ext.auth.impl.jose.JWT;
import net.bytle.crypto.CryptoHmac;
import net.bytle.type.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * A json token class that ciphers Json data with two tokens implementation:
 * * JWT (Json Web Token)
 * * and a more secret Json token algorithm (to cache data from web log parsing on get url property)
 *
 * <p>
 * We use it now for:
 * * publication verification: we send an email with the jwt as query property
 * We may use it for an OLT link
 * <p>
 */
public class JsonToken {

  static Logger LOGGER = LogManager.getLogger(JsonToken.class);

  /**
   * The version (ie secret, algo and cipher)
   * if we lost or want to change any, we can just change it
   */
  public static final String ENCRYPT_SCHEME_VERSION = "1";
  public static final String SECRET_URL_DATA_ENCRYPTION_CONF = "secret.url.data.encryption";

  private final JWT jwt;
  private final JWTOptions jwtOptions;
  private final CryptoHmac hmac;
  private final Base64.Encoder urlBase64Encoder = Base64.getUrlEncoder().withoutPadding();
  private final Base64.Decoder urlBase64Decoder = Base64.getUrlDecoder();

  public JsonToken(String secret) {

    /**
     * Create the JWT encryption
     * The default signature method for JWT’s is known as HS256. HS stands in this case for HMAC Signature using SHA256.
     * This is the simplest key to load
     */
    jwt = new JWT();
    PubSecKeyOptions pubSecKey = new PubSecKeyOptions()
      .setAlgorithm("HS256")
      .setBuffer(secret);
    jwt.addJWK(new JWK(pubSecKey));

    jwtOptions = new JWTOptions();
    jwtOptions.setExpiresInMinutes(60 * 24);
    jwtOptions.setAlgorithm("HS256");

    /**
     * The HMAC cipher
     */
    hmac = CryptoHmac.create(secret)
      .useSha256();

    LOGGER.info("The Json Token utility was initialized.");

  }


  public String encrypt(JsonObject jsonObject, JsonTokenCipher cipher) {
    switch (cipher) {
      case JWS_BASE64:
      case JWS_CLEAR:
        String jwtToken = this.jwt.sign(jsonObject, this.jwtOptions);
        if (cipher.equals(JsonTokenCipher.JWS_CLEAR)) {
          return jwtToken;
        }
        /**
         * We make it a little more difficult to find
         * the data to not leak info:
         * and encode it in base64
         */
        return urlBase64Encoder.encodeToString(jwtToken.getBytes());
      case BUFFER_BYTES:
        byte[] data = jsonObject.toBuffer().getBytes();
        String dataBase64 = urlBase64Encoder.encodeToString(data);
        String dataHmacBase64hmacString = urlBase64Encoder.encodeToString(hmac.encrypt(data));
        String token = ENCRYPT_SCHEME_VERSION + "." + dataHmacBase64hmacString + "." + dataBase64;
        return urlBase64Encoder.encodeToString(token.getBytes(StandardCharsets.UTF_8));
      default:
        throw new IllegalStateException("Unexpected value: " + cipher);
    }


  }

  public JsonObject decrypt(String token, JsonTokenCipher cipher) {

    switch (cipher) {
      case JWS_BASE64:
      case JWS_CLEAR:
        if (cipher == JsonTokenCipher.JWS_BASE64) {
          token = new String(urlBase64Decoder.decode(token));
        }
        return this.jwt.decode(token);
      case BUFFER_BYTES:
        String tokenDecoded = new String(urlBase64Decoder.decode(token));
        List<String> strings = Strings.createFromString(tokenDecoded).split(".");
        // String version = strings.get(0);
        String hmacBase64 = strings.get(1);
        String dataBase64 = strings.get(2);
        byte[] data = urlBase64Decoder.decode(dataBase64);
        String hmacCalculated = urlBase64Encoder.encodeToString(this.hmac.encrypt(data));
        if (!hmacCalculated.equals(hmacBase64)) {
          throw new IllegalStateException("The payload is not valid: " + cipher);
        }
        Buffer buffer = Buffer.buffer(data);
        return new JsonObject(buffer);
      default:
        throw new IllegalStateException("Unexpected value: " + cipher);
    }
  }


  public static class config {

    private String secret;


    public config(ConfigAccessor configAccessor) {

      this.secret = configAccessor.getString(SECRET_URL_DATA_ENCRYPTION_CONF);
    }

    public config setSecret(String secret) {
      this.secret = secret;
      return this;
    }


    public JsonToken create() throws ConfigIllegalException {

      if (this.secret == null) {
        throw new ConfigIllegalException("A secret is mandatory. You can set set in the conf with the attribute (" + SECRET_URL_DATA_ENCRYPTION_CONF + ")");
      }

      return new JsonToken(this.secret);

    }
  }
}
