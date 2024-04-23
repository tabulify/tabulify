package net.bytle.vertx.jackson;

import com.fasterxml.jackson.databind.JsonDeserializer;
import net.bytle.exception.CastException;

public abstract class JacksonJsonStringDeserializer<T> extends JsonDeserializer<T> {


  /**
   *
   * @param value - the input string
   * @return the object
   * We add this function to be able to use them also outside jackson
   */
  public abstract T deserialize(String value) throws CastException;

  /**
   * Fail when the data is a literal or come from the database
   */
  public T deserializeFailSafe(String value) {
    try {
      return deserialize(value);
    } catch (CastException e) {
      throw new RuntimeException("The value (" + value + ") could not be deserialized", e);
    }
  }

}
