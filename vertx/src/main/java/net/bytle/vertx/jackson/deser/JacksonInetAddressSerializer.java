package net.bytle.vertx.jackson.deser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.bytle.vertx.jackson.JacksonJsonStringSerializer;

import java.io.IOException;
import java.net.InetAddress;

public class JacksonInetAddressSerializer extends JacksonJsonStringSerializer<InetAddress> {

  @Override
  public void serialize(InetAddress value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    gen.writeString(serialize(value));
  }


  @Override
  public String serialize(InetAddress inetAddress) {
    return inetAddress.getHostAddress();
  }


}
