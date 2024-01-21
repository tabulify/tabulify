package net.bytle.vertx;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.type.time.Timestamp;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {

  static private DateTimeFormatter defaultFormat() {
    return DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  }

  static public String LocalDateTimetoString(LocalDateTime localDateTime) {
    return localDateTime.format(defaultFormat());
  }

  static public class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
      gen.writeString(DateTimeUtil.LocalDateTimetoString(value));
    }

  }

  public static class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctx) throws IOException, JacksonException {
      String value = p.getValueAsString();
      try {
        return LocalDateTime.parse(value, defaultFormat());
      } catch (Exception e) {
        // Old time data may be stored as 2023-08-24
        try {
          return Timestamp.createFromString(value).toLocalDateTime();
        } catch (CastException ex) {
          throw new InternalException("The value (" + value + ") could not be parsed as LocalDateTime", ex);
        }
      }
    }

  }

  /**
   * @return the now time in UTC
   */
  public static LocalDateTime getNowInUtc() {
    return Timestamp.createFromNowUtc().toLocalDateTime();
  }


}
