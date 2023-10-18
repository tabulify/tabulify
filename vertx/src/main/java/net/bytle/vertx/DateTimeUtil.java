package net.bytle.vertx;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.type.time.Timestamp;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {

  static private DateTimeFormatter defaultFormat() {
    return DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  }

  static public class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
      gen.writeString(value.format(defaultFormat()));
    }

  }

  public static class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctx) throws IOException, JacksonException {
      return LocalDateTime.parse(p.getValueAsString(), defaultFormat());
    }

  }

  /**
   * @return the now time in UTC
   */
  public static LocalDateTime getNowUtc() {
    return Timestamp.createFromNowUtc().toLocalDateTime();
  }



}
