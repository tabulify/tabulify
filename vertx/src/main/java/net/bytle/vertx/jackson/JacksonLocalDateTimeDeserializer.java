package net.bytle.vertx.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import net.bytle.exception.CastException;
import net.bytle.type.time.Timestamp;
import net.bytle.vertx.DateTimeService;

import java.io.IOException;
import java.time.LocalDateTime;

public class JacksonLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
  @Override
  public LocalDateTime deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
    String value = p.getValueAsString();
    if (!value.endsWith("Z")) {
      throw new IOException("The API allows only UTC ISO date time. The value (" + value + ") is incorrect because it has not the `Z` character at the end indicating the UTC zone.");
    }
    value = value.substring(0, value.length() - 1);
    try {
      return LocalDateTime.parse(value, DateTimeService.defaultFormatter());
    } catch (Exception e) {
      // Old time data may be stored as 2023-08-24
      try {
        return Timestamp.createFromString(value).toLocalDateTime();
      } catch (CastException ex) {
        throw new IOException("The value (" + value + ") could not be parsed as LocalDateTime", ex);
      }
    }
  }

}
