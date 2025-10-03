package com.tabulify.service;


import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

/**
 * Service-provider class that represents a system
 * ie Docker, Kubernetes, ...
 */
public abstract class ServiceProvider {

  // lock using when loading providers
  private static final Object lock = new Object();

  // installed providers
  private static volatile List<ServiceProvider> installedConnectionProviders;

  // Used to avoid recursive loading of installed providers
  private static boolean loadingProviders = false;

  private static Void checkPermission() {
    SecurityManager sm = System.getSecurityManager();
    if (sm != null)
      sm.checkPermission(new RuntimePermission("SystemProvider"));
    return null;
  }

  private ServiceProvider(Void ignore) {
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
  protected ServiceProvider() {
    this(checkPermission());
  }

  /**
   * @return the installed providers
   */
  private static List<ServiceProvider> loadInstalledProviders() {

    List<ServiceProvider> connectionProviders = new ArrayList<>();

    ServiceLoader<ServiceProvider> loadedTableSystemProviders = ServiceLoader
      .load(ServiceProvider.class, ClassLoader.getSystemClassLoader());

    for (ServiceProvider provider : loadedTableSystemProviders) {
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
  public static List<ServiceProvider> installedProviders() {
    if (installedConnectionProviders == null) {

      synchronized (lock) {
        if (installedConnectionProviders == null) {
          if (loadingProviders) {
            throw new Error("Circular loading of installed providers detected");
          }
          loadingProviders = true;

          List<ServiceProvider> list = AccessController
            .doPrivileged((PrivilegedAction<List<ServiceProvider>>) ServiceProvider::loadInstalledProviders);

          installedConnectionProviders = Collections.unmodifiableList(list);
        }
      }
    }
    return installedConnectionProviders;
  }


  /**
   * @param name - the name as attribute. Why an attribute, to get the origin on the attribute
   * @return The system
   * @throws SecurityException If a security manager is installed, and it denies an unspecified
   *                           permission.
   */
  public abstract Service createService(Tabular tabular, Attribute name);

  /**
   * @param type the system type
   * @return If the data store extension accept this type
   */
  public abstract boolean accept(String type);


}
