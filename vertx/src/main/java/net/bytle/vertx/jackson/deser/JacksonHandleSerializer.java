package net.bytle.vertx.jackson.deser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.type.Handle;
import net.bytle.vertx.jackson.JacksonJsonStringSerializer;

import java.io.IOException;

public class JacksonHandleSerializer extends JacksonJsonStringSerializer<Handle> {

  @Override
  public void serialize(Handle value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    gen.writeString(serialize(value));
  }


  @Override
  public String serialize(Handle value) {
    if (value == null) {
      return null;
    }
    return value.getValue();
  }

}
