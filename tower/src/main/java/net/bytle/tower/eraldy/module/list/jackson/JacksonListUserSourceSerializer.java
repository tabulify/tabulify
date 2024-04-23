package net.bytle.tower.eraldy.module.list.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.tower.eraldy.model.openapi.ListUserSource;
import net.bytle.vertx.jackson.JacksonJsonStringSerializer;

import java.io.IOException;

public class JacksonListUserSourceSerializer extends JacksonJsonStringSerializer<ListUserSource> {

  @Override
  public void serialize(ListUserSource value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    gen.writeString(serialize(value));
  }

  @Override
  public String serialize(ListUserSource value) {
    return value.name();
  }

}
