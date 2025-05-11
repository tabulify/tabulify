package com.tabulify.sample;


import com.tabulify.spi.DataPath;

import java.util.List;

public interface SchemaSample {

    /**
     * @return all tables
     */
    List<DataPath> createDataPaths();

    /**
     * @param tableName
     * @return one table
     * <p>
     * The table name must be public constant in each class
     * to allow getting one table
     */
    DataPath getAndCreateDataPath(String tableName);


    List<DataPath> getDataPaths(String... tableNames);

}
