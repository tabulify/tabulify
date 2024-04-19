package net.bytle.tower.eraldy.module.user.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.type.EmailAddress;

import java.io.IOException;

public class JacksonEmailAddressSerializer extends JsonSerializer<EmailAddress> {

  @Override
  public void serialize(EmailAddress value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    String normalizedString = value.toNormalizedString();
    gen.writeString(normalizedString);
  }

}
