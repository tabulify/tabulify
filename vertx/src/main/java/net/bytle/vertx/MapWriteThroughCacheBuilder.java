package net.bytle.vertx;

import static java.util.Objects.requireNonNull;

/**
 * A write-through map to cache data from a store
 * (not finished)
 * The write-through should be an interface for the read and write
 * of the object, sot that we don't need to know where the object is located
 * To do:
 * * delete jdbc
 * * create an interface
 */
public class MapWriteThroughCacheBuilder {



  public MapWriteThroughCacheBuilder() {


  }

  public MapConfig<Object, Object> newMapBuilder(){
    return new MapConfig<>(this);
  }

  public static class MapConfig<K,V> {


    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final MapWriteThroughCacheBuilder cacheBuilder;

    protected MapWriteThroughSink<K,V> sink;

    public MapConfig(MapWriteThroughCacheBuilder cacheBuilder) {
      this.cacheBuilder = cacheBuilder;
    }

    public <K1 extends K, V1 extends V> MapConfig<K1,V1> setSink(MapWriteThroughSink<K1,V1> sink) {

      //noinspection unchecked
      MapConfig<K1, V1> self = (MapConfig<K1, V1>) this;
      self.sink = requireNonNull(sink);
      return self;

    }

    public MapWriteThroughCache<K, V> build() {

      /**
       * When an object is serializable, the table can be of the following format
       *  <jdbc:id-column name="ID" type="VARCHAR(255)"/>
       *  <jdbc:data-column name="DATA" type="BYTE"/>
       *  <jdbc:timestamp-column name="TS" type="BIGINT"/>
       *  <jdbc:segment-column name="S" type="INT"/>
       *  <jdbc:segment-column name="VERSION" type="VARCHAR(10)"/>
       */
      return new MapWriteThroughCache<>(this);

    }



  }

}
