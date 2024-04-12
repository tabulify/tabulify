package net.bytle.tower.eraldy.module.mailing.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.module.mailing.model.MailingStatus;

import java.io.IOException;

public class JacksonMailingStatusDeserializer extends JsonDeserializer<MailingStatus> {
  @Override
  public MailingStatus deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
    int statusCode;
    try {
      statusCode = Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new IOException("The status code ("+value+") is not a number",e);
    }
    try {
      return MailingStatus.fromStatusCode(statusCode);
    } catch (NotFoundException e) {
      throw new IOException(e);
    }

  }

}
