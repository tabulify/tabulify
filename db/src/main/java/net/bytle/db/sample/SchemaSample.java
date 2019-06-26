package net.bytle.db.sample;

import net.bytle.db.model.TableDef;

import java.util.List;

public interface SchemaSample {

    /**
     * @return all tables
     */
    List<TableDef> getTables();

    /**
     * @param tableName
     * @return one table
     * <p>
     * The table name must be public constant in each class
     * to allow getting one table
     */
    TableDef getTable(String tableName);


    List<TableDef> getTables(String... tableNames);
}
