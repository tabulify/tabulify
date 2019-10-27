package net.bytle.db.jdbc.spi;

/**
 * Created by gerard on 02-12-2015.
 */

import net.bytle.db.jdbc.URIExtended;

import java.net.URI;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Factory methods for works. This class defines factory methods to
 * construct other types of works.
 *
 *
 * <p> The first invocation of the {@link SqlDatabaseProvider#installedProviders
 * installedProviders} method, by way of invoking any of the {@code
 * newWork} methods defined by this class, locates and loads all
 * installed file system providers.
 *
 * Installed providers are loaded using the
 * service-provider loading facility defined by the {@link ServiceLoader} class.
 * Installed providers are loaded using the system class loader. If the
 * system class loader cannot be found then the extension class loader is used;
 * if there is no extension class loader then the bootstrap class loader is used.
 *
 * Providers are typically installed by placing them in a JAR file on the
 * application class path or in the extension directory, the JAR file contains a
 * provider-configuration file named {@code work.WorkProvider}
 * in the resource directory {@code META-INF/services}, and the file lists one or
 * more fully-qualified names of concrete subclass of {@link SqlDatabaseProvider}
 * that have a zero argument constructor.
 *
 * The ordering that installed providers are located is implementation specific.
 * If a provider is instantiated and its {@link SqlDatabaseProvider#getServer()
 * getServer} returns the same URI scheme of a provider that was previously
 * instantiated then the most recently instantiated duplicate is discarded. URI
 * schemes are compared without regard to case. If
 * circular loading of installed providers is detected then an unspecified error
 * is thrown.
 *
 * <p> This class also defines factory methods that allow a {@link ClassLoader}
 * to be specified when locating a provider. As with installed providers, the
 * provider classes are identified by placing the provider configuration file
 * in the resource directory {@code META-INF/services}.
 *
 * <p> If a thread initiates the loading of the installed file system providers
 * and another thread invokes a method that also attempts to load the providers
 * then the method will block until the loading completes.
 *
 */

public final class SqlDatabases {


    private SqlDatabases() {
    }


    /**
     * Returns a reference to an existing {@code Work}.
     *
     * <p> This method iterates over the {@link SqlDatabaseProvider#installedProviders()
     * installed} providers to locate the provider that is identified by the URI
     * {@link URI#getScheme scheme} of the given URI. URI schemes are compared
     * without regard to case. The exact form of the URI is highly provider
     * dependent. If found, the provider's {@link SqlDatabaseProvider#getSqlDatabase(String)}
     * getWork} method is invoked to obtain a reference to the {@code
     * Work}.
     *
     *
     * <p> If a security manager is installed then a provider implementation
     * may require to check a permission before returning a reference to an
     * existing work.
     *
     * @param   uri  the URI to locate the Sql Database
     *
     * @return  the sql database or the default ansi extension
     *
     * @throws  IllegalArgumentException
     *          if the pre-conditions for the {@code uri} parameter are not met
     * @throws  SecurityException
     *          if a security manager is installed and it denies an unspecified
     *          permission
     */
    public static SqlDatabase getSqlDatabase(String uri) {
        URIExtended uriExtended = new URIExtended(uri);
        String server = uriExtended.getServer();

        for (SqlDatabaseProvider provider: SqlDatabaseProvider.installedProviders()) {
            if (server.equalsIgnoreCase(provider.getServer())) {
                return provider.getSqlDatabase(uri);
            }
        }
        throw new RuntimeException("Provider for the server \"" + server + "\" not found");
    }

    /**
     * Constructs a new work that is identified by a {@link URI}
     *
     * <p> This method iterates over the {@link SqlDatabaseProvider#installedProviders()
     * installed} providers to locate the provider that is identified by the URI
     * {@link URI#getScheme scheme} of the given URI. URI schemes are compared
     * without regard to case. The exact form of the URI is highly provider
     * dependent. If found, the provider's {@link SqlDatabaseProvider#getSqlDatabase(String)}
     * newWork(URI,Map)} method is invoked to construct the new file system.
     *
     *
     * <p> <b>Usage Example:</b>
     * Suppose there is a provider identified by the scheme {@code "sftp"}
     * installed:
     * <pre>
     *   Map&lt;String,String&gt; env = new HashMap&lt;&gt;();
     *   env.put("path", "/my/path");
     *   env.put("host", "/myhost");
     *   Work work = Works.newWork(URI.create("sftp:///?name=logfs"), env);
     * </pre>
     *
     * @param   uri
     *          the URI identifying the file system
     * @param   env
     *          a map of provider specific properties to configure the file system;
     *          may be empty
     *
     * @return  a new file system
     *
     * @throws  IllegalArgumentException
     *          if the pre-conditions for the {@code uri} parameter are not met,
     *          or the {@code env} parameter does not contain properties required
     *          by the provider, or a property value is invalid
     * @throws  SecurityException
     *          if a security manager is installed and it denies an unspecified
     *          permission required by the file system provider implementation
     */
    public static SqlDatabase getSqlDatabase(String uri, Map<String,?> env)
    {
        return getSqlDatabase(uri, env, null);
    }

    /**
     * Constructs a new work that is identified by a {@link URI}
     *
     * <p> This method first attempts to locate an installed provider in exactly
     * the same manner as the {@link #getSqlDatabase(String, Map)}
     * method. If none of the installed providers support the URI scheme then an
     * attempt is made to locate the provider using the given class loader. If a
     * provider supporting the URI scheme is located then its {@link
     * SqlDatabaseProvider#getSqlDatabase(String, Map)}  is
     * invoked to construct the new work.
     *
     * @param   uri
     *          the URI identifying the file system
     * @param   env
     *          a map of provider specific properties to configure the file system;
     *          may be empty
     * @param   loader
     *          the class loader to locate the provider or {@code null} to only
     *          attempt to locate an installed provider
     *
     * @return  a new work
     *
     * @throws  IllegalArgumentException
     *          if the pre-conditions for the {@code uri} parameter are not met,
     *          or the {@code env} parameter does not contain properties required
     *          by the provider, or a property value is invalid
     * @throws  ServiceConfigurationError
     *          when an error occurs while loading a service provider
     * @throws  SecurityException
     *          if a security manager is installed and it denies an unspecified
     *          permission required by the file system provider implementation
     */
    public static SqlDatabase getSqlDatabase(String uri, Map<String,?> env, ClassLoader loader) {
        URIExtended uriExtended = new URIExtended(uri);
        String server = uriExtended.getServer();

        // check installed providers
        for (SqlDatabaseProvider provider: SqlDatabaseProvider.installedProviders()) {
            if (server.equalsIgnoreCase(provider.getServer())) {
                return provider.getSqlDatabase(uri, env);
            }
        }

        // if not found, use service-provider loading facility
        if (loader != null) {
            ServiceLoader<SqlDatabaseProvider> sl = ServiceLoader
                    .load(SqlDatabaseProvider.class, loader);
            for (SqlDatabaseProvider provider: sl) {
                if (server.equalsIgnoreCase(provider.getServer())) {
                    return provider.getSqlDatabase(uri, env);
                }
            }
        }

        throw new RuntimeException("Provider \"" + server + "\" not found");
    }


}
