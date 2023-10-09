package net.bytle.db.memory;


import net.bytle.type.MediaType;

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
public abstract class MemoryVariableManagerProvider {

    // lock using when loading providers
    private static final Object lock = new Object();

    // installed providers
    private static volatile List<MemoryVariableManagerProvider> installedStructProviders;

    // Used to avoid recursive loading of installed providers
    private static boolean loadingProviders = false;

    private static Void checkPermission() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkPermission(new RuntimePermission("MemoryVariableManagerProvider"));
        return null;
    }

    private MemoryVariableManagerProvider(Void ignore) {
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
    protected MemoryVariableManagerProvider() {
        this(checkPermission());
    }

    // loads all installed providers
    private static List<MemoryVariableManagerProvider> loadInstalledProviders() {

        List<MemoryVariableManagerProvider> memoryVariableManagerProviders = new ArrayList<>();

        ServiceLoader<MemoryVariableManagerProvider> loadedTableSystemProviders = ServiceLoader
                .load(MemoryVariableManagerProvider.class, ClassLoader.getSystemClassLoader());

        // ServiceConfigurationError may be throw here
        for (MemoryVariableManagerProvider provider : loadedTableSystemProviders) {

            memoryVariableManagerProviders.add(provider);

        }

        return memoryVariableManagerProviders;
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
    public static List<MemoryVariableManagerProvider> installedProviders() {
        if (installedStructProviders == null) {

            synchronized (lock) {
                if (installedStructProviders == null) {
                    if (loadingProviders) {
                        throw new Error("Circular loading of installed providers detected");
                    }
                    loadingProviders = true;

                    List<MemoryVariableManagerProvider> list = AccessController
                            .doPrivileged((PrivilegedAction<List<MemoryVariableManagerProvider>>) () -> loadInstalledProviders());

                    installedStructProviders = Collections.unmodifiableList(list);
                }
            }
        }
        return installedStructProviders;
    }

    /**
     *
     * @return true if the variable manager accepts the type
     * @param type
     */
    public abstract Boolean accept(MediaType type);

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
     * @throws SecurityException           If a security manager is installed and it denies an unspecified
     *                                     permission.
     */
    public abstract MemoryVariableManager getMemoryVariableManager();


    /**
     * Others providers methods if needed
     */
}
