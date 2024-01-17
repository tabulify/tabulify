package net.bytle.tower.eraldy.api.implementer.util;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import net.bytle.tower.eraldy.model.openapi.ListUserFlow;

import java.io.IOException;

public class ListUserUtil {

  public static class FlowDeserializer extends JsonDeserializer<ListUserFlow> {
    @Override
    public ListUserFlow deserialize(JsonParser p, DeserializationContext ctx) throws IOException, JacksonException {
      String value = p.getValueAsString();
      switch (value) {
        case "oauth":
          return ListUserFlow.OAUTH;
        case "import":
          return ListUserFlow.IMPORT;
        case "email":
          return ListUserFlow.EMAIL;
        default:
          return ListUserFlow.fromValue(Integer.valueOf(value));
      }
    }

  }

}
