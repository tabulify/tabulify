package net.bytle.tower.eraldy.module.common.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.tower.eraldy.model.manual.Status;
import net.bytle.vertx.jackson.JacksonJsonStringSerializer;

import java.io.IOException;

/**
 * Old serializer used during the transition to GraphQL
 * to output the status has code
 */
public class JacksonStatusSerializer extends JacksonJsonStringSerializer<Status> {



  @Override
  public void serialize(Status value, JsonGenerator gen, SerializerProvider serializers) throws IOException {


    gen.writeString(serialize(value));

  }

  @Override
  public String serialize(Status value) {
    return value.getName();
  }
}
