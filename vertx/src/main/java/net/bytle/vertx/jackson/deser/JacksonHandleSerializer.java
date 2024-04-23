package net.bytle.vertx.jackson.deser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.type.Handle;

import java.io.IOException;

public class JacksonHandleSerializer extends JsonSerializer<Handle> {

  @Override
  public void serialize(Handle value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    gen.writeString(value.getValueOrNull());
  }


}
