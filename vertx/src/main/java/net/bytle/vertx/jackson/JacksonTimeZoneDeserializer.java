package net.bytle.vertx.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;

import java.io.IOException;
import java.util.TimeZone;

public class JacksonTimeZoneDeserializer extends StdScalarDeserializer<TimeZone> {
  protected JacksonTimeZoneDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public TimeZone deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    String value = p.getValueAsString();
    return TimeZone.getTimeZone(value);
  }

}
