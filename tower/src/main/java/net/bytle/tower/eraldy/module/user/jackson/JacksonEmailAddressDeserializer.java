package net.bytle.tower.eraldy.module.user.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import net.bytle.type.EmailAddress;
import net.bytle.type.EmailCastException;

import java.io.IOException;

public class JacksonEmailAddressDeserializer extends JsonDeserializer<EmailAddress> {
  @Override
  public EmailAddress deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
    try {
      return EmailAddress.of(value);
    } catch (EmailCastException e) {
      throw new IOException("The email address (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

  }

}
