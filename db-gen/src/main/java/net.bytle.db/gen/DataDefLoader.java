package net.bytle.db.gen;


import net.bytle.cli.Log;
import net.bytle.db.engine.Dag;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;

import java.util.*;

/**
 * The input of the data generation is in a yml file
 */
public class DataDefLoader {

    private final SchemaDef schemaDef;


    // Default number of rows to load
    private Integer defaultRows = 100;

    // Do we load a parent table if no rows are present
    private Boolean loadParent = false;

    // The data gen property by fully qualified table name
    // Its build on the object scope to not pass it around in the functions signature
    private Map<String, DataDef> dataDefMap = new HashMap<>();

    // A cache to know which tables was loaded
    private List<TableDef> loadedTables = new ArrayList<>();

    private static final Log LOGGER = Gen.GEN_LOG;

    /**
     *
     * @param schemaDef the schema object
     */
    public DataDefLoader(SchemaDef schemaDef) {

        this.schemaDef = schemaDef;

    }

    public static DataDefLoader of(SchemaDef schemaDef) {
        return new DataDefLoader(schemaDef);
    }

    /**
     * By default, if no rows is specified how many rows do we need to load
     * Default: 100
     *
     * @param rows
     * @return
     */
    public DataDefLoader defaultRows(Integer rows) {
        this.defaultRows = rows;
        return this;
    }

    /**
     * If a table defined in the property file has a parent table defined via its foreign key
     * and that the parent table has no rows, do we load the table also with generated data.
     * Default to false (ie no)
     *
     * @param loadParent
     * @return
     */
    public DataDefLoader loadParentTable(Boolean loadParent) {
        this.loadParent = loadParent;
        return this;
    }

    public List<TableDef> load(DataDef dataDef) {
        return load(Arrays.asList(dataDef));
    }

    public List<TableDef> load(List<DataDef> dataDefs) {

        // Create a list of table to load to be able to
        // get the load order
        // And create a map of data def because the load is not done via a graph but recursive
        List<TableDef> tableDefs = new ArrayList<>();
        for (DataDef dataDef:dataDefs) {

            String fullyQualifiedName = schemaDef.getDatabase().getObjectBuilder().getFullyQualifiedName(dataDef.getTable(), schemaDef.getName());
            dataDefMap.put(fullyQualifiedName, dataDef);
            tableDefs.add(schemaDef.getTableOf(dataDef.getTable()));

        }

        // Load the tables
        for (TableDef tableDef: Dag.get(tableDefs).getCreateOrderedTables()) {

            load(tableDef);

        }

        // The load order is determined by the constraint
        // TODO: Create a graph to see the flow ?
        return loadedTables;

    }

    private void load(TableDef tableDef) {

        // A load of a table may trigger the load of a foreign table
        // We check then if the table was already loaded
        // Already loaded ?
        if (loadedTables.contains(tableDef)) {
            LOGGER.fine("The table " + tableDef.getFullyQualifiedName() + ") was already loaded.");
            return;
        }

        LOGGER.fine("Loading the table " + tableDef.getFullyQualifiedName() + ")");

        // Parent ?
        // If yes, load the parent first
        if (tableDef.getForeignKeys().size() != 0) {
            for (ForeignKeyDef foreignKeyDef : tableDef.getForeignKeys()) {
                TableDef parentTableDef = foreignKeyDef.getForeignPrimaryKey().getTableDef();
                if (!loadedTables.contains(parentTableDef)) {

                    Integer rows = Tables.getSize(parentTableDef);
                    if (rows == 0) {
                        if (this.loadParent) {
                            load(parentTableDef);
                        } else {
                            throw new RuntimeException("The table (" + tableDef.getFullyQualifiedName() + ") has a foreign key to the parent table (" + parentTableDef.getFullyQualifiedName() + "). This table has no rows and the option to load parent is disabled, we cannot then generated rows in the table (" + tableDef.getFullyQualifiedName() + ")");
                        }
                    }

                }
            }

        }

        // Property
        DataDef dataDef = dataDefMap.get(tableDef.getFullyQualifiedName());

        // Rows and gen properties
        Integer rows = this.defaultRows;
        Map<String, Map<String, Object>> columnsProperties = null;
        if (dataDef != null) {

            if (dataDef.getRows() != null) {
                rows = dataDef.getRows();
            }

            columnsProperties = dataDef.getColumns();
        }

        // Load
        DataGenLoader.get(tableDef)
                .setRows(rows)
                .properties(columnsProperties)
                .load();

        // Add the table in the loaded table
        // to stop the recursive calls
        // in case of cycle when a parent table need to be loaded
        loadedTables.add(tableDef);

    }

}
