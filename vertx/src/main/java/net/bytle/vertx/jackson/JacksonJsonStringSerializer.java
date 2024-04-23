package net.bytle.vertx.jackson;

import com.fasterxml.jackson.databind.JsonSerializer;

public abstract class JacksonJsonStringSerializer<T> extends JsonSerializer<T> {


  /**
   *
   * @param value - the input string
   * @return the object
   * We add this function to be able to use them also outside jackson
   */
  public abstract String serialize(T value);

}
