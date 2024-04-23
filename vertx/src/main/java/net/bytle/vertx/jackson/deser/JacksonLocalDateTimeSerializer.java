package net.bytle.vertx.jackson.deser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.jackson.JacksonJsonStringSerializer;

import java.io.IOException;
import java.time.LocalDateTime;

public class JacksonLocalDateTimeSerializer extends JacksonJsonStringSerializer<LocalDateTime> {
  @Override
  public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    gen.writeString(serialize(value));
  }

  @Override
  public String serialize(LocalDateTime value) {
    return DateTimeService.LocalDateTimetoString(value);
  }

}
