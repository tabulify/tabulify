package net.bytle.vertx.jackson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.json.jackson.DatabindCodec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Manage the separate ObjectMapper instances
 * <p>
 * Why?
 * Jackson defines a set of per-mapper configuration, which can ONLY be defined before using ObjectMapper --
 * meaning that these settings can not be changed on-the-fly, on per-request basis.
 * They configure fundamental POJO introspection details, and resulting built objects
 * (serializers, deserializers, related) are heavily cached.
 * If you need differing settings for these, you have to use separate ObjectMapper instances.
 * See <a href="https://github.com/FasterXML/jackson-databind/wiki/Mapper-Features">Ref</a>
 */
public class JacksonMapperManager {

  static Logger LOGGER = LogManager.getLogger(JacksonMapperManager.class);
  private final JavaTimeModule javaTimeModule;

  public JacksonMapperManager() {
    javaTimeModule = new JavaTimeModule();
    javaTimeModule.addSerializer(LocalDateTime.class, new JacksonLocalDateTimeSerializer());
    javaTimeModule.addDeserializer(LocalDateTime.class, new JacksonLocalDateTimeDeserializer());
  }

  /**
   * Jackson
   * Add support for local date time serialization
   * Behind the scene Vertx uses Jackson's ObjectMapper inside encode() and encodePrettily() methods.
   * <p>
   * We use it to support LocalDateTime as we store all date time in UTC format
   * <p>
   * Ref: <a href="https://vertx.io/docs/4.1.8/vertx-sql-client-templates/java/#_java_datetime_api_mapping">Ref</a>
   */
  public static JacksonMapperManager create() {

    return new JacksonMapperManager();

  }


  public JavaTimeModule getJavaTimeModule() {

    return javaTimeModule;
  }

  public void enableTimeModuleForVertx() {
    Arrays.asList(
        DatabindCodec.mapper(),
        DatabindCodec.prettyMapper()
      )
      .forEach(mapper -> mapper.registerModule(getJavaTimeModule()));
    LOGGER.info("Date time in JSON jackson enabled for Vertx Codec");
  }

  /**
   * @return a json mapper
   * <p>
   * Note: It seems that the {@link ObjectMapper#addMixIn(Class, Class) Jackson mixins} cannot be added or deleted
   * after the first used of a mapper, we create
   * therefore the mappers in the start phase of the server
   */
  public JsonMapperBuilder jsonMapperBuilder() {

    return new JsonMapperBuilder();
  }

  public class JsonMapperBuilder {

    private boolean disableFailOnUnknownProperties = false;
    private final HashMap<Class<?>, Class<?>> mixins = new HashMap<>();
    private boolean disableFailOnEmptyBeans = false;

    public JsonMapperBuilder setDisableFailOnUnknownProperties() {
      this.disableFailOnUnknownProperties = true;
      return this;
    }

    public JsonMapper build() {
      JsonMapper mapper = JsonMapper
        .builder()
        .build();
      mapper.registerModule(getJavaTimeModule());
      if (disableFailOnUnknownProperties) {
        /**
         * To avoid unrecognizable field when we develop if we have stored a json
         * field that was deleted later
         * Unrecognized field \"owner\" (class net.bytle.tower.eraldy.model.openapi.Realm), not marked as ignorable
         */
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      }
      if (disableFailOnEmptyBeans) {
        /**
         * When the bean/pojo is empty, the default behaviour is to failed
         * It may happen with a server event because the name is a property hard coded
         * in a class
         */
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
      }
      if (!this.mixins.isEmpty()) {
        for (Map.Entry<Class<?>, Class<?>> entry : this.mixins.entrySet()) {
          mapper.addMixIn(entry.getKey(), entry.getValue());
        }
      }
      return mapper;
    }

    /**
     *
     * Mixins permits to change the Jackson Metadata on the fly
     * by overwriting the properties
     */
    public JsonMapperBuilder addMixIn(Class<?> originalClass, Class<?> mixinClass) {
      this.mixins.put(originalClass, mixinClass);
      return this;
    }

    public JsonMapperBuilder disableFailOnEmptyBeans() {
      this.disableFailOnEmptyBeans = true;
      return this;
    }
  }
}
