package net.bytle.tower.eraldy.module.list.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.model.openapi.ListUserSource;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;

public class JacksonListUserSourceDeserializer extends JacksonJsonStringDeserializer<ListUserSource> {
  @Override
  public ListUserSource deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
    String value = p.getValueAsString();
    try {
      return deserialize(value);
    } catch (CastException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ListUserSource deserialize(String value) throws CastException {
    switch (value) {
      case "oauth":
        return ListUserSource.OAUTH;
      case "import":
        return ListUserSource.IMPORT;
      case "email":
        return ListUserSource.EMAIL;
      default:
        return ListUserSource.fromValue(Integer.valueOf(value));
    }
  }
}
