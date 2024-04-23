package net.bytle.vertx.jackson.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.type.EmailAddress;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;

public class JacksonEmailAddressDeserializer extends JacksonJsonStringDeserializer<EmailAddress> {
  @Override
  public EmailAddress deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
    try {
      return deserialize(value);
    } catch (CastException e) {
      throw new IOException("The email address (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

  }

  @Override
  public EmailAddress deserialize(String s) throws CastException {
    return EmailAddress.of(s);
  }
}
