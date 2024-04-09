package net.bytle.vertx.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;
import java.util.TimeZone;

public class JacksonTimeZoneSerializer extends StdScalarSerializer<TimeZone> {
  protected JacksonTimeZoneSerializer(Class<TimeZone> t) {
    super(t);
  }

  @Override
  public void serialize(TimeZone value, JsonGenerator gen, SerializerProvider provider) throws IOException {
    String id = value.getID();
    gen.writeString(id);
  }

}
