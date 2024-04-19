package net.bytle.tower.eraldy.module.user.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import net.bytle.type.time.TimeZoneCast;
import net.bytle.type.time.TimeZoneUtil;

import java.io.IOException;
import java.util.TimeZone;

public class JacksonTimeZoneDeserializer extends JsonDeserializer<TimeZone> {
  @Override
  public TimeZone deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
    try {
      return TimeZoneUtil.getTimeZoneWithValidation(value);
    } catch (TimeZoneCast e) {
      throw new IOException("The time zone (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

  }

}
