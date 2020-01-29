package net.bytle.db.fs;

import net.bytle.db.database.DataStore;
import net.bytle.db.database.FileDataStore;
import net.bytle.db.spi.TableSystemProvider;

import java.util.Arrays;
import java.util.List;

public class FsTableSystemProvider extends TableSystemProvider {


    public static final String LOCAL_FILE_SCHEME = "file";

    // TODO: get them from the NIOS implementation
    public static final String HTTP_SCHEME = "http";
    public static final String HTTP_SCHEMES = "https";


    private static FsTableSystemProvider defaultTableSystemProvider;

    public static FsTableSystemProvider getDefault() {
        if (defaultTableSystemProvider == null){
            defaultTableSystemProvider = new FsTableSystemProvider();
        }
        return defaultTableSystemProvider;
    }

    /**
     * Returns the URI scheme that identifies this provider.
     *
     * @return The URI scheme
     */
    @Override
    public List<String> getSchemes() {
        return Arrays.asList(LOCAL_FILE_SCHEME, HTTP_SCHEME, HTTP_SCHEMES);
    }



    /**
     * Returns an existing {@code work} created by this provider.
     *
     * <br>
     * If a security manager is installed then a provider implementation
     * may require to check a permission before returning a reference to an
     * existing work.
     *
     * @param dataStore URI reference
     * @return The table system
     * @throws SecurityException If a security manager is installed and it denies an unspecified
     *                           permission.
     */
    @Override
    public FsTableSystem getTableSystem(DataStore dataStore) {
        return FsTableSystem.of(this, (FileDataStore) dataStore);
    }


}
