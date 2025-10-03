package com.tabulify.spi;


import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.connection.Connection;
import com.tabulify.service.Service;

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
 * A factory method class defines how providers are located
 * and loaded.
 * The first invocation of a method (such as installedTableSystemProviders)
 * locates and loads all installed file system providers.
 * Installed providers are loaded using the service-provider loading facility defined by the ServiceLoader class.
 * Installed providers are loaded using the system class loader.
 * If the system class loader cannot be found then the extension class loader is used; if there is no extension class loader then the bootstrap class loader is used.
 * <p/>
 * Default parameters may be overridden by setting a system property.
 * <p/>
 * All providers have generally zero argument constructor that initializes the provider.
 * <p/>
 * <p/>
 * Inspired by {@link java.nio.file.spi.FileSystemProvider}
 */
public abstract class ConnectionProvider {

  // lock using when loading providers
  private static final Object lock = new Object();

  // installed providers
  private static volatile List<ConnectionProvider> installedConnectionProviders;

  // Used to avoid recursive loading of installed providers
  private static boolean loadingProviders = false;

  private static Void checkPermission() {
    SecurityManager sm = System.getSecurityManager();
    if (sm != null)
      sm.checkPermission(new RuntimePermission("ConnectionProvider"));
    return null;
  }

  private ConnectionProvider(Void ignore) {
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
  protected ConnectionProvider() {
    this(checkPermission());
  }

  /**
   *
   * @return the installed providers
   */
  private static List<ConnectionProvider> loadInstalledProviders() {

    List<ConnectionProvider> connectionProviders = new ArrayList<>();

    ServiceLoader<ConnectionProvider> loadedTableSystemProviders = ServiceLoader
      .load(ConnectionProvider.class, ClassLoader.getSystemClassLoader());

    for (ConnectionProvider provider : loadedTableSystemProviders) {
      connectionProviders.add(provider);
    }

    return connectionProviders;
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
  public static List<ConnectionProvider> installedProviders() {
    if (installedConnectionProviders == null) {

      synchronized (lock) {
        if (installedConnectionProviders == null) {
          if (loadingProviders) {
            throw new Error("Circular loading of installed providers detected");
          }
          loadingProviders = true;

          List<ConnectionProvider> list = AccessController
            .doPrivileged((PrivilegedAction<List<ConnectionProvider>>) ConnectionProvider::loadInstalledProviders);

          installedConnectionProviders = Collections.unmodifiableList(list);
        }
      }
    }
    return installedConnectionProviders;
  }


  /**
   * Returns an existing {@code work} created by this provider.
   * <p/>
   * The work is identified by its {@code URI}. Its exact form
   * is highly provider dependent.
   * <p/>
   * <p> If a security manager is installed then a provider implementation
   * may require to check a permission before returning a reference to an
   * existing work.
   *
   * @return The sql database
   * @throws SecurityException If a security manager is installed, and it denies an unspecified
   *                           permission.
   */
  public abstract Connection createConnection(Tabular tabular, Attribute name, Attribute uri);

  /**
   *
   * @param uri the uri variable
   * @return If the data store extension accept this url
   */
  public abstract boolean accept(Attribute uri);


  /**
   * @return a howto services
   */
  public Set<Service> getHowToServices(Tabular tabular) {
    return new HashSet<>();
  }

  /**
   * @return a howto connections
   */
  public Set<Connection> getHowToConnections(Tabular tabular) {
    return new HashSet<>();
  }

}
