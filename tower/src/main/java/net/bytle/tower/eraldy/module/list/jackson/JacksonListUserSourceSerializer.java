package net.bytle.tower.eraldy.module.list.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.tower.eraldy.model.openapi.ListUserSource;

import java.io.IOException;

public class JacksonListUserSourceSerializer extends JsonSerializer<ListUserSource> {

  @Override
  public void serialize(ListUserSource value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    gen.writeString(value.name());
  }

}
