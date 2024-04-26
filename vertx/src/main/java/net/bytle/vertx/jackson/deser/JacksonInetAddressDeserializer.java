package net.bytle.vertx.jackson.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import net.bytle.exception.CastException;
import net.bytle.vertx.jackson.JacksonJsonStringDeserializer;
import org.xbill.DNS.Address;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class JacksonInetAddressDeserializer extends JacksonJsonStringDeserializer<InetAddress> {
  @Override
  public InetAddress deserialize(JsonParser p, DeserializationContext ctx) throws IOException {

    String value = p.getValueAsString();
    try {
      return deserialize(value);
    } catch (CastException e) {
      throw new IOException("The inet/ip address (" + value + ") is not valid. Error: " + e.getMessage(), e);
    }

  }

  @Override
  public InetAddress deserialize(String s) throws CastException {
    if (s == null) {
      return null;
    }
    try {
      return Address.getByAddress(s);
    } catch (UnknownHostException e) {
      throw new CastException("The value (" + s + ") is not a valid inet/ip address. Error: " + e.getMessage(), e);
    }
  }
}
