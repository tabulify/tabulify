package net.bytle.tower.eraldy.module.realm.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.module.realm.model.UserStatus;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;

import java.io.IOException;

public class JacksonUserStatusDeserializer extends JacksonJsonStringDeserializer<UserStatus> {
  @Override
  public UserStatus deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
    try {
      return deserialize(value);
    } catch (CastException e) {
      throw new IOException(e);
    }

  }

  @Override
  public UserStatus deserialize(String value) throws CastException {
    int statusCode;
    try {
      statusCode = Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new CastException("The status code (" + value + ") is not a number", e);
    }
    try {
      return UserStatus.fromStatusCode(statusCode);
    } catch (NotFoundException e) {
      throw new CastException("The status code (" + value + ") is not valid", e);
    }
  }
}
