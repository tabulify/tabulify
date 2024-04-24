package net.bytle.vertx.jackson.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.type.Handle;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;

public class JacksonHandleDeserializer extends JacksonJsonStringDeserializer<Handle> {
  @Override
  public Handle deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
    try {
      return deserialize(value);
    } catch (CastException e) {
      throw new IOException("The handle value (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

  }

  @Override
  public Handle deserialize(String s) throws CastException {
    return Handle.of(s);
  }

}
