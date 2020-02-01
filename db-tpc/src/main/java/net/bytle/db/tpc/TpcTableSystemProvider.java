package net.bytle.db.tpc;

import net.bytle.db.database.DataStore;
import net.bytle.db.spi.TableSystemProvider;

import java.util.Arrays;
import java.util.List;

public class TpcTableSystemProvider extends TableSystemProvider {


    public static final String TPCDS_SCHEME = "tpcds";



    /**
     * Returns the URI scheme that identifies this provider.
     *
     * @return The URI scheme
     */
    @Override
    public List<String> getSchemes() {
        return Arrays.asList(TPCDS_SCHEME);
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
    public TpcDataSetSystem getTableSystem(DataStore dataStore) {
        return TpcDataSetSystem.of(this, dataStore);
    }


}
