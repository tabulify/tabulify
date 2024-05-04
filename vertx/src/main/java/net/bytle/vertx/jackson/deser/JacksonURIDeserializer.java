package net.bytle.vertx.jackson.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;
import java.net.URI;

public class JacksonURIDeserializer extends JacksonJsonStringDeserializer<URI> {
  @Override
  public URI deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
    try {
      return deserialize(value);
    } catch (CastException e) {
      throw new IOException("The URI value (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

  }

  @Override
  public URI deserialize(String s) throws CastException {
    if (s == null) {
      throw new CastException("The string should not be null");
    }
    try {
      return URI.create(s);
    } catch (IllegalArgumentException e) {
      throw new CastException(e.getMessage(), e);
    }
  }
}
