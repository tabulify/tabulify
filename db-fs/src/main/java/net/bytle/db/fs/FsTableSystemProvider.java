package net.bytle.db.fs;

import net.bytle.db.spi.TableSystem;
import net.bytle.db.spi.TableSystemProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FsTableSystemProvider extends TableSystemProvider {


    /**
     * Returns the URI scheme that identifies this provider.
     *
     * @return The URI scheme
     */
    @Override
    public List<String> getSchemes() {
        return Arrays.asList("file","sftp");
    }

    /**
     * Constructs a new {@code Work} object identified by a URI. This
     * method is invoked by the {@link #getTableSystem(String, Map)}
     * method to open a new work identified by a URI.
     * <p/>
     * <p> The {@code uri} parameter is an absolute, hierarchical URI, with a
     * scheme equal (without regard to case) to the scheme supported by this
     * provider. The exact form of the URI is highly provider dependent. The
     * {@code env} parameter is a map of provider specific properties to configure
     * the work.
     * <p/>
     * <p> This method may throws an exception if the
     * work already exists because it was previously created by an
     * invocation of this method.
     *
     * @param uri URI reference
     * @param env A map of provider specific properties to configure the file system;
     *            may be empty
     * @return A new work
     */
    @Override
    public TableSystem getTableSystem(String uri, Map<String, ?> env) {
       return new FsTableSystem();
    }

    /**
     * Returns an existing {@code work} created by this provider.
     * <p/>
     * <p> This method returns a reference to a {@code work} that was
     * created by invoking the {@link #getTableSystem(String, Map)}
     * method.
     * The work is identified by its {@code URI}. Its exact form
     * is highly provider dependent.
     * <p/>
     * <p> If a security manager is installed then a provider implementation
     * may require to check a permission before returning a reference to an
     * existing work.
     *
     * @param uri URI reference
     * @return The sql database
     * @throws SecurityException If a security manager is installed and it denies an unspecified
     *                           permission.
     */
    @Override
    public TableSystem getTableSystem(String uri) {
        return null;
    }


}
