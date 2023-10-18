package net.bytle.tower.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.json.jackson.DatabindCodec;
import net.bytle.java.JavaEnvs;
import net.bytle.vertx.DateTimeUtil;

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

  /**
   * Jackson
   * Add support for local date time serialization
   * Behind the scene Vertx uses Jackson's ObjectMapper inside encode() and encodePrettily() methods.
   * <p>
   * We use it to support LocalDateTime as we store all date time in UTC format
   * FI: This code should have been added when vertx starts (ie {@link net.bytle.tower.MainVerticle}
   * <p>
   * Ref: <a href="https://vertx.io/docs/4.1.8/vertx-sql-client-templates/java/#_java_datetime_api_mapping">Ref</a>
   */
  public static void initVertxJacksonMapper() {


    Arrays.asList(
        DatabindCodec.mapper(),
        DatabindCodec.prettyMapper()
      )
      .forEach(JacksonMapperManager::initModule);


  }

  private static void initModule(ObjectMapper mapper) {



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

  public static JavaTimeModule getJavaTimeModule() {
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    javaTimeModule.addSerializer(LocalDateTime.class, new DateTimeUtil.LocalDateTimeSerializer());
    javaTimeModule.addDeserializer(LocalDateTime.class, new DateTimeUtil.LocalDateTimeDeserializer());
    return javaTimeModule;
  }

}
