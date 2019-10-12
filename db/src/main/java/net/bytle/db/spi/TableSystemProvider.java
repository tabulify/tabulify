package net.bytle.db.spi;


import net.bytle.db.database.Database;

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
public abstract class TableSystemProvider {

    // lock using when loading providers
    private static final Object lock = new Object();

    // installed providers
    private static volatile List<TableSystemProvider> installedTableSystemProviders;

    // Used to avoid recursive loading of installed providers
    private static boolean loadingProviders = false;

    private static Void checkPermission() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkPermission(new RuntimePermission("TableSystemProvider"));
        return null;
    }

    private TableSystemProvider(Void ignore) {
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
    protected TableSystemProvider() {
        this(checkPermission());
    }

    // loads all installed providers
    private static List<TableSystemProvider> loadInstalledProviders() {

        List<TableSystemProvider> tableSystemProviders = new ArrayList<>();

        ServiceLoader<TableSystemProvider> loadedTableSystemProviders = ServiceLoader
                .load(TableSystemProvider.class, ClassLoader.getSystemClassLoader());

        // TODO: validate the provider ?
        // ServiceConfigurationError may be throw here
        for (TableSystemProvider provider : loadedTableSystemProviders) {

            // Validate the provider ?
            // List<String> schemes = provider.getSchemes();
            tableSystemProviders.add(provider);

        }

        return tableSystemProviders;
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
    public static List<TableSystemProvider> installedProviders() {
        if (installedTableSystemProviders == null) {

            synchronized (lock) {
                if (installedTableSystemProviders == null) {
                    if (loadingProviders) {
                        throw new Error("Circular loading of installed providers detected");
                    }
                    loadingProviders = true;

                    List<TableSystemProvider> list = AccessController
                            .doPrivileged((PrivilegedAction<List<TableSystemProvider>>) () -> loadInstalledProviders());

                    installedTableSystemProviders = Collections.unmodifiableList(list);
                }
            }
        }
        return installedTableSystemProviders;
    }

    /**
     * Returns the URI scheme that identifies this provider.
     *
     * @return The URI scheme
     */
    public abstract List<String> getSchemes();

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
     * @param database : an object that got all Uri information
     * @return The sql database
     * @throws SecurityException           If a security manager is installed and it denies an unspecified
     *                                     permission.
     */
    public abstract TableSystem getTableSystem(Database database);


    /**
     * Others providers methods if needed
     */
}
