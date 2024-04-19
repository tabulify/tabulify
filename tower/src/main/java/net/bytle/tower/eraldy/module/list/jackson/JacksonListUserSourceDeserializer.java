package net.bytle.tower.eraldy.module.list.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import net.bytle.tower.eraldy.model.openapi.ListUserSource;

import java.io.IOException;

public class JacksonListUserSourceDeserializer extends JsonDeserializer<ListUserSource> {
  @Override
  public ListUserSource deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
    String value = p.getValueAsString();
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
