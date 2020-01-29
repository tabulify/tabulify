package net.bytle.db.jdbc;

import net.bytle.db.database.DataStore;
import net.bytle.db.database.Database;
import net.bytle.db.spi.TableSystem;
import net.bytle.db.spi.TableSystemProvider;

import java.util.Arrays;
import java.util.List;

public class JdbcDataSystemProvider extends TableSystemProvider {
    /**
     * Returns the URI scheme that identifies this provider.
     *
     * @return The URI scheme
     */
    @Override
    public List<String> getSchemes() {
        return Arrays.asList("jdbc");
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
     * @param database : an object that got all Uri information
     * @return The sql database
     * @throws SecurityException If a security manager is installed and it denies an unspecified
     *                           permission.
     */
    @Override
    public TableSystem getTableSystem(DataStore database) {

      return JdbcDataSystem.of((Database) database);

    }
}
