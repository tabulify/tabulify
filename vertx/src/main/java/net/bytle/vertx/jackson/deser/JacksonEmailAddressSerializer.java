package net.bytle.vertx.jackson.deser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.type.EmailAddress;
import net.bytle.vertx.jackson.JacksonJsonStringSerializer;

import java.io.IOException;

public class JacksonEmailAddressSerializer extends JacksonJsonStringSerializer<EmailAddress> {

  @Override
  public void serialize(EmailAddress value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    gen.writeString(serialize(value));
  }


  @Override
  public String serialize(EmailAddress value) {
    return value.toNormalizedString();
  }
}
