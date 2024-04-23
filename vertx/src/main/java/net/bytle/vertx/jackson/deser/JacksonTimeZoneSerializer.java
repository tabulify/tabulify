package net.bytle.vertx.jackson.deser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.vertx.jackson.JacksonJsonStringSerializer;

import java.io.IOException;
import java.util.TimeZone;

public class JacksonTimeZoneSerializer extends JacksonJsonStringSerializer<TimeZone> {

  @Override
  public void serialize(TimeZone value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

    gen.writeString(serialize(value));
  }

  @Override
  public String serialize(TimeZone value) {
    return value.getID();
  }

}
