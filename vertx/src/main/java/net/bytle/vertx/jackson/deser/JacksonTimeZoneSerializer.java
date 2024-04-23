package net.bytle.vertx.jackson.deser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.TimeZone;

public class JacksonTimeZoneSerializer extends JsonSerializer<TimeZone> {

  @Override
  public void serialize(TimeZone value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    String normalizedString = value.getID();
    gen.writeString(normalizedString);
  }

}
