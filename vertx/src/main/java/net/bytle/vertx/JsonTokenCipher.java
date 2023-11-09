package net.bytle.vertx;

public enum JsonTokenCipher {

  /**
   * Same idea than JWT but
   * with the data being an object serialization
   * and the algo being unknown
   * <p>
   * As this method rely on object serialization, it will break
   * if there is a code change.
   * This method should then be used to encrypt data with
   * a short TTL such as sending an email validation link
   */
  BUFFER_BYTES,
  /**
   * A JWS is passed encoded in base64
   * In case, the JWS is passed in a URL property.
   * To make it harder to decode as the data payload can leak as this is a known methodology
   */
  JWS_BASE64,
  /**
   * A JWS is passed in clear
   * to give back in post
   */
  JWS_CLEAR

}
