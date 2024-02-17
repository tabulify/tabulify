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

/**
 * Mapdb is a techno that permits to manage
 * value in a map that is backed by a persistent storage
 * <p>
 * We use now to persist the sessions.
 * <p>
 * Weakness: Hold techno. It will return a new instance when you iterate over the values of
 * a cache. Meaning that you can't pass status update that
 * will persist over a simple loop.
 * The project was not updated for a couple of years.
 */
public class MapDb extends TowerService {

  private static final String MAPDB_FILE_HOME_CONF = "mapdb.file.home";
  static Logger LOGGER = LogManager.getLogger(MapDb.class);

  private final DB mapDb;
  private final JsonMapper mapper;


  public MapDb(Server server) {
    DB mapDbTemp;

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

    DBMaker.Maker maker = DBMaker.fileDB(dbPath.toFile())
      .closeOnJvmShutdown()
      .transactionEnable();
    try {
      mapDbTemp = maker
        .make();
    } catch (org.mapdb.DBException e) {
      if (JavaEnvs.IS_DEV) {
        mapDbTemp = maker
          /**
           * org.mapdb.DBException$DataCorruption: Header checksum broken.
           *  Store was not closed correctly and might be corrupted. Use `DBMaker.checksumHeaderBypass()`
           *  to recover your data
           *  Use clean shutdown or enable transactions to protect the store in the future.
           */
          .checksumHeaderBypass()
          /**
           * org.mapdb.DBException$FileLocked: File is already opened and is locked: build\mapdb\mapdb
           * Locking happens with FileChannel.tryLock
           */
          .fileLockDisable()
          .make();
      } else {
        throw e;
      }
    }
    mapDb = mapDbTemp;
    mapper = server
      .getJacksonMapperManager()
      .jsonMapperBuilder()
      .setDisableFailOnUnknownProperties()
      .build();
  }


  public void close() {

    mapDb.close();

  }

  @SuppressWarnings("unused")
  public <K, V> DB.HashMapMaker<K, V> hashMapWithJsonValueObject(String name, Serializer<K> keySerializer, Class<V> valueClass) {

    MapDbJacksonSerializer<V> valueSerializer = new MapDbJacksonSerializer<>(valueClass, mapper);
    return mapDb.hashMap(name, keySerializer, valueSerializer);
  }

  public <K, V> DB.HashMapMaker<K, V> hashMap(String name, Serializer<K> keySerializer, Serializer<V> valueSerializer) {
    return mapDb.hashMap(name, keySerializer, valueSerializer);
  }

  public void commit() {
    this.mapDb.commit();
  }



}
