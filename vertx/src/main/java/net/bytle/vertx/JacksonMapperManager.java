package net.bytle.vertx;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.json.jackson.DatabindCodec;
import net.bytle.java.JavaEnvs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.Arrays;

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
    javaTimeModule.addSerializer(LocalDateTime.class, new DateTimeUtil.LocalDateTimeSerializer());
    javaTimeModule.addDeserializer(LocalDateTime.class, new DateTimeUtil.LocalDateTimeDeserializer());
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

  public void initMapper(ObjectMapper mapper) {

    if (JavaEnvs.IS_DEV) {
      /**
       * To avoid unrecognizable field when we develop if we have stored a json
       * field that was deleted later
       * Unrecognized field \"owner\" (class net.bytle.tower.eraldy.model.openapi.Realm), not marked as ignorable
       */
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    mapper.registerModule(getJavaTimeModule());

  }

  public JavaTimeModule getJavaTimeModule() {

    return javaTimeModule;
  }

  public void enableTimeModuleForVertx() {
    Arrays.asList(
        DatabindCodec.mapper(),
        DatabindCodec.prettyMapper()
      )
      .forEach(this::initMapper);
    LOGGER.info("Date time in JSON jackson enabled");
  }

  /**
   * @return a json mapper
   * <p>
   * Note: It seems that the {@link ObjectMapper#addMixIn(Class, Class) Jackson mixins} cannot be added or deleted
   * after the first used of a mapper, we create
   * therefore the mappers in the start phase of the server
   */
  public JsonMapper createNewJsonMapper() {
    JsonMapper mapper = JsonMapper
      .builder()
      .build();
    this.initMapper(mapper);
    return mapper;
  }
}
