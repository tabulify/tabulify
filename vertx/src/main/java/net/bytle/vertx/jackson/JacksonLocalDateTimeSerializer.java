package net.bytle.vertx.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.vertx.DateTimeService;

import java.io.IOException;
import java.time.LocalDateTime;

public class JacksonLocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
  @Override
  public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    String localDateTime = DateTimeService.LocalDateTimetoString(value);
    gen.writeString(localDateTime);
  }

}
