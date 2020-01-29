package net.bytle.db.sample;


import net.bytle.db.spi.DataPath;

import java.util.List;

public interface SchemaSample {

    /**
     * @return all tables
     */
    List<DataPath> getAndCreateDataPaths();

    /**
     * @param tableName
     * @return one table
     * <p>
     * The table name must be public constant in each class
     * to allow getting one table
     */
    DataPath getAndCreateDataPath(String tableName);


    List<DataPath> getAndCreateDataPaths(String... tableNames);

}
