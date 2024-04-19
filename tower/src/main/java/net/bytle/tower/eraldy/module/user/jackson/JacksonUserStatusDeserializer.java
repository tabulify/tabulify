package net.bytle.tower.eraldy.module.user.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.module.user.model.UserStatus;

import java.io.IOException;

public class JacksonUserStatusDeserializer extends JsonDeserializer<UserStatus> {
  @Override
  public UserStatus deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
    int statusCode;
    try {
      statusCode = Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new IOException("The status code ("+value+") is not a number",e);
    }
    try {
      return UserStatus.fromStatusCode(statusCode);
    } catch (NotFoundException e) {
      throw new IOException("The status code ("+value+") is not valid",e);
    }

  }

}
