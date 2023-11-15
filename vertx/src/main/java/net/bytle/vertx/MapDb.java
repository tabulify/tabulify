package net.bytle.vertx;

import com.fasterxml.jackson.databind.json.JsonMapper;
import net.bytle.fs.Fs;
import net.bytle.java.JavaEnvs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.nio.file.Path;
import java.nio.file.Paths;

public class MapDb implements AutoCloseable {

  private static final String MAPDB_FILE_HOME_CONF = "mapdb.file.home";
  static Logger LOGGER = LogManager.getLogger(MapDb.class);

  private final DB mapDb;
  private final JsonMapper mapper;


  public MapDb(Server server) {

    String defaultHome = "mapdb/";
    if (JavaEnvs.IS_DEV) {
      defaultHome = "build/" + defaultHome;
    }
    String homeStringConfValue = server.getConfigAccessor().getString(MAPDB_FILE_HOME_CONF);
    String homeString;
    if (homeStringConfValue == null) {
      homeString = defaultHome;
      LOGGER.info("Map db enabled with the default home value of (" + homeString + "). The conf (" + MAPDB_FILE_HOME_CONF + ") was not found");
    } else {
      homeString = homeStringConfValue;
      LOGGER.info("Map db enabled with the home value of (" + homeString + ") from the conf (" + MAPDB_FILE_HOME_CONF + ")");
    }
    Path dbPath = Paths.get(homeString + "mapdb");
    LOGGER.info("Map db database located at: " + dbPath.toAbsolutePath());
    Fs.createDirectoryIfNotExists(dbPath.getParent());
    mapDb = DBMaker.fileDB(dbPath.toFile()).make();

    mapper = server.getJacksonMapperManager().jsonMapperBuilder()
      .setDisableFailOnUnknownProperties()
      .build();
  }


  public void close() {
    mapDb.close();
  }

  public <K, V> DB.HashMapMaker<K, V> hashMap(String name, Serializer<K> keySerializer, Class<V> valueClass) {

    MapDbJacksonSerializer<V> valueSerializer = new MapDbJacksonSerializer<>(valueClass,mapper);
    return mapDb.hashMap(name, keySerializer, valueSerializer);
  }

}
