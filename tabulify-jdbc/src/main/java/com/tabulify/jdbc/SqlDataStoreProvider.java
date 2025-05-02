
package com.tabulify.jdbc;

import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.conf.Attribute;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

/**
 * Service-provider class
 * <p/>
 * A service is normally just the definition of a method/interface.
 * <p/>
 * The methods defined in a static class will typically delegate to an instance of this
 * class.
 * <p/>
 * A service provider is a concrete implementation of this class that
 * implements the abstract methods defined by this class.
 * <p/>
 * A provider is identified by a {@code URI} {@link #accept(Attribute)}.
 * <p/>
 * A factory method class defines how providers are located
 * and loaded.
 * The first invocation of a method (such as installedProviders)
 * locates and loads all installed file system providers.
 * Installed providers are loaded using the service-provider loading facility defined by the ServiceLoader class.
 * Installed providers are loaded using the system class loader.
 * If the system class loader cannot be found then the extension class loader is used; if there is no extension class loader then the bootstrap class loader is used.
 * <p/>
 * Default parameters may be overridden by setting a system property.
 * <p/>
 * All providers have generally zero argument constructor that initializes the provider.
 * <p/>
 * A provider is a factory for one or more (service) object instances.
 * Each service is identified by a {@code URI} where the URI's scheme matches
 * the provider's {@link #accept(Attribute)}.
 * <p/>
 * Inspired by {@link java.nio.file.spi.FileSystemProvider}
 */

public abstract class SqlDataStoreProvider {

  // lock using when loading providers
  private static final Object lock = new Object();

  // installed providers
  private static volatile List<SqlDataStoreProvider> installedProviders;

  // used to avoid recursive loading of installed providers
  private static boolean loadingProviders = false;

  private static Void checkPermission() {
    SecurityManager sm = System.getSecurityManager();
    if (sm != null)
      sm.checkPermission(new RuntimePermission("fileSystemProvider"));
    return null;
  }

  private SqlDataStoreProvider(Void ignore) {
  }

  /**
   * Initializes a new instance of this class.
   * <p/>
   * <p> During construction a provider may safely access files associated
   * with the default provider but care needs to be taken to avoid circular
   * loading of other installed providers. If circular loading of installed
   * providers is detected then an unspecified error is thrown.
   *
   * @throws SecurityException If a security manager has been installed and it denies
   *                           {@link RuntimePermission}<tt>("fileSystemProvider")</tt>
   */
  protected SqlDataStoreProvider() {
    this(checkPermission());
  }

  // loads all installed providers
  private static List<SqlDataStoreProvider> loadInstalledProviders() {

    List<SqlDataStoreProvider> list = new ArrayList<>();

    ServiceLoader<SqlDataStoreProvider> jdbcDataStoreExtensionProviders = ServiceLoader
      .load(SqlDataStoreProvider.class, ClassLoader.getSystemClassLoader());

    for (SqlDataStoreProvider provider : jdbcDataStoreExtensionProviders) {
        list.add(provider);
    }

    return list;
  }

  /**
   * Returns a list of the installed work providers.
   * <p/>
   * <p> The first invocation of this method loads any installed
   * providers that will be used by the provider Factory class.
   *
   * @return An unmodifiable list of the installed service providers.
   * @throws ServiceConfigurationError When an error occurs while loading a service provider
   */
  public static List<SqlDataStoreProvider> installedProviders() {
    if (installedProviders == null) {

      synchronized (lock) {
        if (installedProviders == null) {
          if (loadingProviders) {
            throw new Error("Circular loading of installed providers detected");
          }
          loadingProviders = true;

          List<SqlDataStoreProvider> list = AccessController
            .doPrivileged((PrivilegedAction<List<SqlDataStoreProvider>>) SqlDataStoreProvider::loadInstalledProviders);

          installedProviders = Collections.unmodifiableList(list);
        }
      }
    }
    return installedProviders;
  }



  /**
   * @param name the connection name
   * @param uri the url
   * @return a data store created by the extension
   */
  public abstract Connection getJdbcDataStore(Tabular tabular, Attribute name, Attribute uri);

  /**
   *
   * @param uri
   * @return true if the provider is accepting the URL
   */
  public abstract boolean accept(Attribute uri);

}
