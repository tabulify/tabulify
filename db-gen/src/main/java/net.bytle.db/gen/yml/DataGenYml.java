package net.bytle.db.gen.yml;


import net.bytle.db.gen.DataGenLoader;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The input of the data generation is in a yml file
 */
public class DataGenYml {

    private final SchemaDef schemaDef;


    // Default number of rows to load
    private Integer defaultRows = 100;

    // Do we load a parent table if no rows are present
    private Boolean loadParent = false;

    // The data gen property by fully qualified table name
    // Its build on the object scope to not pass it around in the functions signature
    private Map<String, DataGenYmlProperty> propertiesByTable = new HashMap<>();

    // A cache to know which tables was loaded
    private List<TableDef> loadedTables = new ArrayList<>();

    private static final Logger LOGGER = Logger.getLogger(DataGenYml.class.getPackage().toString());

    /**
     *
     * @param schemaDef the schema object
     * @param input an input stream that points to an YAML file that describe the load properties.
     */
    public DataGenYml(SchemaDef schemaDef, InputStream input) {

        this.schemaDef = schemaDef;
        if (input==null){
            throw new RuntimeException("The input stream of the Yaml file must not be null");
        }
        // Transform the file in properties
        Constructor constructor = new Constructor(DataGenYmlProperty.class);
        Yaml yaml = new Yaml(constructor);
        Iterable<Object> dataObject = yaml.loadAll(input);
        for (Object data : dataObject) {
            final DataGenYmlProperty dataGenYmlProperty = (DataGenYmlProperty) data;
            String fullyQualifiedName = schemaDef.getDatabase().getObjectBuilder().getFullyQualifiedName(dataGenYmlProperty.getTable(), schemaDef.getName());
            propertiesByTable.put(fullyQualifiedName, dataGenYmlProperty);
        }

    }

    /**
     * By default, if no rows is specified how many rows do we need to load
     * Default: 100
     *
     * @param rows
     * @return
     */
    public DataGenYml defaultRows(Integer rows) {
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
    public DataGenYml loadParentTable(Boolean loadParent) {
        this.loadParent = loadParent;
        return this;
    }

    public List<TableDef> load() {


        // Load the tables
        for (DataGenYmlProperty dataGenYmlProperty : propertiesByTable.values()) {

            TableDef tableDef = schemaDef.getTableOf(dataGenYmlProperty.getTable());
            load(tableDef);

        }
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
        DataGenYmlProperty dataGenYmlProperty = propertiesByTable.get(tableDef.getFullyQualifiedName());

        // Rows and gen properties
        Integer rows = this.defaultRows;
        Map<String, Map<String, Object>> columnsProperties = null;
        if (dataGenYmlProperty != null) {

            if (dataGenYmlProperty.getRows() != null) {
                rows = dataGenYmlProperty.getRows();
            }

            columnsProperties = dataGenYmlProperty.getColumns();
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
