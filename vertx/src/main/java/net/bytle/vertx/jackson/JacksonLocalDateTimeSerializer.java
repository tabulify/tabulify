package net.bytle.vertx.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.vertx.DateTimeUtil;

import java.io.IOException;
import java.time.LocalDateTime;

public class JacksonLocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
  @Override
  public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    gen.writeString(DateTimeUtil.LocalDateTimetoString(value));
  }

}
