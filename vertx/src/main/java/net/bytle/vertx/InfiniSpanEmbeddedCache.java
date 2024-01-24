package net.bytle.vertx;

import net.bytle.java.JavaEnvs;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

import java.nio.file.Path;
import java.nio.file.Paths;

public class InfiniSpanEmbeddedCache implements AutoCloseable {


  private final DefaultCacheManager cacheManager;
  private final Path homePath;

  public InfiniSpanEmbeddedCache(Server server) {

    server.addCloseableService(this);


    String homeDirectory = "infinispan";
    if (JavaEnvs.IS_DEV) {
      homeDirectory = "build/" + homeDirectory;
    }
    this.homePath = Paths.get(homeDirectory);


    // https://infinispan.org/docs/stable/titles/embedding/embedding.html
    // Set up a clustered Cache Manager.
    // For the PERMANENT flag to take effect, you must enable global state and set a configuration storage provider.
    GlobalConfiguration global = GlobalConfigurationBuilder.defaultClusteredBuilder()
      .globalState()
      .enable()
      .persistentLocation(this.homePath.toFile().getAbsolutePath())
      .build();


    // Initialize the default Cache Manager.
    // Cache Managers are heavyweight objects and Infinispan recommends instantiating only one instance per JVM.
    cacheManager = new DefaultCacheManager(global);
    // cacheManager.start() method to initialize a CacheManager before you can create caches.
    cacheManager.start();


  }

  public <K, V> Cache<K, V> getOrCreatePersistentCache(String cacheName){

    // https://infinispan.org/docs/stable/titles/configuring/configuring.html#configuring-file-stores_persistence
    Configuration configuration = new ConfigurationBuilder()
      .persistence()
      .passivation(false) // save evicted entries to cache store
      .addSoftIndexFileStore()
      .dataLocation(System.getProperty("java.io.tmpdir") + "infinispan")
      .build();
    return cacheManager
      .administration() // embedded cache manager
      // Obtain a permanent cache.
      // the PERMANENT flag (for caches to survive restarts)
      // is deprecated as this is the default
      .getOrCreateCache(cacheName, configuration);
  }



  @Override
  public void close() throws Exception {
    // Stop the Cache Manager.
    // release JVM resources and gracefully shutdown any caches.
    cacheManager.stop();
  }

}
