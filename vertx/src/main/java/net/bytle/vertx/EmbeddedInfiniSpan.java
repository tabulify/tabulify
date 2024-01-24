package net.bytle.vertx;

import net.bytle.java.JavaEnvs;
import org.infinispan.Cache;
import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Embedded InfiniSpan
 * The start time is really heavy (3,4 minutes)
 */
public class EmbeddedInfiniSpan implements AutoCloseable {


  private final DefaultCacheManager cacheManager;
  private final Path homePath;

  public EmbeddedInfiniSpan(Configurator builder) {

    if(builder.server!=null){
      builder.server.addCloseableService(this);
    }

    String homeDirectory = "infinispan";
    if (JavaEnvs.IS_DEV) {
      homeDirectory = "build/" + homeDirectory;
    }
    this.homePath = Paths.get(homeDirectory);

    /**
     * Cache Manager
     */
    // https://infinispan.org/docs/stable/titles/embedding/embedding.html
    // Set up a clustered Cache Manager.
    // For the PERMANENT flag to take effect, you must enable global state and set a configuration storage provider.

    GlobalConfigurationBuilder globalBuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
    // Global state configuration
    globalBuilder
      .globalState()
      .enable() // enable
      .persistentLocation(homePath.toFile().getAbsolutePath()); // default storage location

    // Initialize the Cache Manager.
    // Cache Managers are heavyweight objects and Infinispan recommends instantiating only one instance per JVM.
    GlobalConfiguration globalConfiguration = globalBuilder.build();
    cacheManager = new DefaultCacheManager(globalConfiguration,  false);
    // cacheManager.start() initialize the CacheManager (Mandatory before using it and creating caches).
    // Take a long time to start
    cacheManager.start();

  }

  public <K, V> Cache<K, V> getOrCreatePersistentCache(String cacheName) {

    /**
     * The cache location must be a child of the home
     */
    String cacheLocation = this.homePath.resolve(cacheName).toFile().getAbsolutePath();

    // https://infinispan.org/docs/stable/titles/configuring/configuring.html#configuring-file-stores_persistence
    ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
    // Persistence configuration
    configurationBuilder
      .persistence()
      .passivation(false) // save evicted entries to cache store
      .addSoftIndexFileStore()
      .dataLocation(cacheLocation);

    return cacheManager
      .administration() // embedded cache manager
      // Obtain a permanent cache.
      // the PERMANENT flag (for caches to survive restarts) is deprecated as this is the default
      // without VOLATILE we got this problem
      // org.infinispan.commons.CacheConfigurationException: ISPN000501: Cannot persist cache configuration as global state is disabled
      .withFlags(CacheContainerAdmin.AdminFlag.VOLATILE)
      .getOrCreateCache(cacheName, configurationBuilder.build());
  }


  @Override
  public void close() throws Exception {
    // Stop the Cache Manager.
    // release JVM resources and gracefully shutdown any caches.
    cacheManager.stop();
  }

  public static Configurator config(){
    return new Configurator();
  }
  public static class Configurator{


    private Server server;

    /**
     * If InfiniSpan runs on a server.
     */
    public Configurator setServer(Server server){
      this.server = server;
      return this;
    }

    public EmbeddedInfiniSpan build(){
      return new EmbeddedInfiniSpan(this);
    }


  }

}
