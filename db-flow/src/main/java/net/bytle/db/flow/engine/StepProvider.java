package net.bytle.db.flow.engine;


import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

/**
 *
 */
public abstract class StepProvider {

  // lock using when loading providers
  private static final Object lock = new Object();

  // installed providers
  private static volatile List<StepProvider> installedStructProviders;

  // Used to avoid recursive loading of installed providers
  private static boolean loadingProviders = false;

  private static Void checkPermission() {
    SecurityManager sm = System.getSecurityManager();
    if (sm != null)
      sm.checkPermission(new RuntimePermission(StepProvider.class.getSimpleName()));
    return null;
  }

  private StepProvider(Void ignore) {
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
  protected StepProvider() {
    this(checkPermission());
  }

  // loads all installed providers
  private static List<StepProvider> loadInstalledProviders() {

    List<StepProvider> stepProviders = new ArrayList<>();

    ServiceLoader<StepProvider> loadedTableSystemProviders =
      ServiceLoader
      .load(StepProvider.class, ClassLoader.getSystemClassLoader());

    // ServiceConfigurationError may be throw here
    for (StepProvider provider : loadedTableSystemProviders) {

      stepProviders.add(provider);

    }

    return stepProviders;
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
  public static List<StepProvider> installedProviders() {
    if (installedStructProviders == null) {

      synchronized (lock) {
        if (installedStructProviders == null) {
          if (loadingProviders) {
            throw new Error("Circular loading of installed providers detected");
          }
          loadingProviders = true;

          List<StepProvider> list = AccessController
            .doPrivileged((PrivilegedAction<List<StepProvider>>) StepProvider::loadInstalledProviders);

          installedStructProviders = Collections.unmodifiableList(list);
        }
      }
    }
    return installedStructProviders;
  }

  /**
   * This is used by the {@link StepProvider}
   * to match a command name to an object
   *
   * @return true if the step accepts the command
   */
  public abstract Boolean accept(String commandName);

  /**
   * @return the accepted operation names
   */
  public abstract Set<String> getAcceptedCommandNames();

  /**
   * This is used by the {@link StepProvider}
   * to create an operation object when {@link #accept(String)} match
   * <p>
   * Create an new instance of the same operation
   */
  public abstract OperationStep createStep();


}
