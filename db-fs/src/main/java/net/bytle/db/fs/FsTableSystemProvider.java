package net.bytle.db.fs;

import net.bytle.db.database.Database;
import net.bytle.db.spi.TableSystem;
import net.bytle.db.spi.TableSystemProvider;

import java.util.Arrays;
import java.util.List;

public class FsTableSystemProvider extends TableSystemProvider {


    /**
     * Returns the URI scheme that identifies this provider.
     *
     * @return The URI scheme
     */
    @Override
    public List<String> getSchemes() {
        return Arrays.asList("file");
    }



    /**
     * Returns an existing {@code work} created by this provider.
     * <p/>
     * <p/>
     * <p> If a security manager is installed then a provider implementation
     * may require to check a permission before returning a reference to an
     * existing work.
     *
     * @param database URI reference
     * @return The sql database
     * @throws SecurityException If a security manager is installed and it denies an unspecified
     *                           permission.
     */
    @Override
    public TableSystem getTableSystem(Database database) {
        return FsTableSystem.of(database);
    }


}
