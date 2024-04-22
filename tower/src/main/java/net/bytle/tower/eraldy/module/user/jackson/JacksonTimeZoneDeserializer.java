package net.bytle.tower.eraldy.module.user.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.type.time.TimeZoneUtil;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;
import java.util.TimeZone;

public class JacksonTimeZoneDeserializer extends JacksonJsonStringDeserializer<TimeZone> {
  @Override
  public TimeZone deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
    try {
      return deserialize(value);
    } catch (CastException e) {
      throw new IOException("The time zone (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

  }

  @Override
  public TimeZone deserialize(String s) throws CastException {
    return TimeZoneUtil.getTimeZoneWithValidation(s);
  }

}
