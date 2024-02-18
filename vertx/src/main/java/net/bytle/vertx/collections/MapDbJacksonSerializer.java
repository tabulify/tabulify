package net.bytle.vertx.collections;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import java.io.IOException;
import java.io.Serializable;

/**
 * A Serializer that uses a Jackson Mapper
 * @param <V> - the type of class to serialize
 */
class MapDbJacksonSerializer<V> implements Serializer<V>, Serializable {

  private final Class<V> clazz;
  private final JsonMapper mapper;

  public MapDbJacksonSerializer(Class<V> clazz, JsonMapper mapper) {
    this.clazz = clazz;
    this.mapper = mapper;
  }

  @Override
  public void serialize(@NotNull DataOutput2 out, @NotNull V value) throws IOException {
    String event = mapper.writeValueAsString(value);
    out.writeUTF(event);
  }

  @Override
  public V deserialize(@NotNull DataInput2 input, int available) throws IOException {
    return mapper.readValue(input.readUTF(),clazz);
  }

}
