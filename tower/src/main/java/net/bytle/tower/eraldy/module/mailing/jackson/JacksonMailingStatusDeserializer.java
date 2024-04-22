package net.bytle.tower.eraldy.module.mailing.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.module.mailing.model.MailingStatus;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;

public class JacksonMailingStatusDeserializer extends JacksonJsonStringDeserializer<MailingStatus> {
  @Override
  public MailingStatus deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
    try {
      return deserialize(value);
    } catch (CastException e) {
      throw new IOException(e);
    }

  }

  @Override
  public MailingStatus deserialize(String value) throws CastException {
    int statusCode;
    try {
      statusCode = Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new CastException("The status code (" + value + ") is not a number", e);
    }
    try {
      return MailingStatus.fromStatusCode(statusCode);
    } catch (NotFoundException e) {
      throw new CastException(e);
    }
  }
}
