package net.bytle.vertx;

import io.vertx.pgclient.PgPool;
import net.bytle.type.KeyNameNormalizer;

public class CaffeinePersistenceManager {

  private final PgPool jdbcPool;

  public CaffeinePersistenceManager(Server server) {
    this.jdbcPool = server.getPostgresDatabaseConnectionPool();

  }

  public <K,V> MapConfig<K,V> mapConfig(String name){
    return new MapConfig<>(this,name);
  }

  public static class MapConfig<K,V> {

    /**
     * read-only
     * drop-on-exit
     * create-on-start
     * table.string.<id|data|timestamp>.name - Specifies the column name.
     * table.string.<id|data|timestamp>.type - Specifies the column type.
     */
    private final String name;
    private final CaffeinePersistenceManager persistenceManager;

    public MapConfig(CaffeinePersistenceManager persistenceManager, String name) {
      this.persistenceManager = persistenceManager;
      this.name = KeyNameNormalizer.createFromString(name).toSqlCase();
    }


    public CaffeinePersistenceMap<K, V> build() {

      /**
       *  <jdbc:id-column name="ID" type="VARCHAR(255)"/>
       *  <jdbc:data-column name="DATA" type="BYTEA"/>
       *  <jdbc:timestamp-column name="TS" type="BIGINT"/>
       *  <jdbc:segment-column name="S" type="INT"/>
       */

      return new CaffeinePersistenceMap<>(this);
    }

  }

}
