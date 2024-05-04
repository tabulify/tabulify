package net.bytle.vertx.jackson.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class JacksonURLDeserializer extends JacksonJsonStringDeserializer<URL> {
  @Override
  public URL deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
    try {
      return deserialize(value);
    } catch (CastException e) {
      throw new IOException("The URL value (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

  }

  @Override
  public URL deserialize(String s) throws CastException {
    try {
      return new URL(s);
    } catch (MalformedURLException e) {
      throw new CastException(e.getMessage(), e);
    }
  }
}
