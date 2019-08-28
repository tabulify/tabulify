package net.bytle.db.gen;

import net.bytle.cli.Log;
import net.bytle.db.engine.Dag;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.*;
import net.bytle.db.stream.SqlInsertStream;
import net.bytle.db.stream.Streams;
import net.bytle.type.Maps;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents an data generation instance
 * <p>
 * This class:
 *   * give context to a data generation builder - help build the data generation (give context, a derived data generator need to get access to the other data generations)
 *   * got all meta necessary to trigger a {@link #load()}
 *
 */
public class DataGeneration {

    /**
     * The {@link TableDef#getProperty(String)} key giving the total number of rows that the table should have
     */
    public static final String TOTAL_ROWS_PROPERTY_KEY = "TotalRows";

    /**
     * The {@link TableDef#getProperty(String)} key giving the data generator data
     */
    public static final String GENERATOR_PROPERTY_KEY = "DataGenerator";

    static final Log GEN_LOG = Log.getLog(DataGeneration.class);
    private static final Log LOGGER = GEN_LOG;


    /**
     * Max records to insert if there is no total rows defined
     */
    private static final Integer MAX_INSERT = 100000;

    /**
     * The table to load with the total number of rows
     */
    private final Map<TableDef, Integer> tablesToLoad = new HashMap<>();
    private final Map<ColumnDef, ForeignKeyDef> columnForeignKeyMap = new HashMap<>();
    private final Map<ColumnDef, UniqueKeyDef> columnUniqueKeyMap = new HashMap<>();
    private List<ColumnDef> primaryColumns = new ArrayList<>();

    /**
     * Do we need to load parent table even if they are not in the set
     */
    private Boolean loadParent;


    private DataGeneration() {




    }

    /**
     * The build has a recursive shape because of the derived data generator that depends on another
     * We used this map to get track of what was build
     */
    private Map<ColumnDef, DataGenerator> dataGenerators = new HashMap<>();

    public DataGeneration addTable(TableDef tableDef, Integer totalRows) {

        if (totalRows==null){
            final Object totalRowsObject = Maps.getPropertyCaseIndependent(tableDef.getProperties(), TOTAL_ROWS_PROPERTY_KEY);
            try {
                totalRows = (Integer) totalRowsObject;
            } catch (ClassCastException e){
                throw new RuntimeException("The total rows property of the table ("+tableDef.getFullyQualifiedName()+") is not an integer. Its value is: "+totalRowsObject);
            }
        }
        // Adding the table into the list of tables to load
        tablesToLoad.put(tableDef, totalRows);

        // Building the foreign, primary and unique keys

        // Self referencing foreign key check
        List<ForeignKeyDef> selfReferencingForeignKeys = DataGens.getSelfReferencingForeignKeys(tableDef);
        if (selfReferencingForeignKeys.size() > 0) {
            for (ForeignKeyDef foreignKeyDef : selfReferencingForeignKeys) {
                LOGGER.severe("The foreign key " + foreignKeyDef.getName() + " on the table (" + foreignKeyDef.getTableDef().getFullyQualifiedName() + ") references itself and it's not supported.");
            }
            throw new RuntimeException("Self referencing foreign key found int he table " + tableDef.getFullyQualifiedName());
        }


        // Primary Key with only one column are supported
        PrimaryKeyDef primaryKeyDef = tableDef.getPrimaryKey();
        // Extract the primary column
        if (primaryKeyDef != null) {
            primaryColumns.addAll(primaryKeyDef.getColumns());
        }

        // Foreign Key with only one column are supported
        List<ForeignKeyDef> foreignKeys = tableDef.getForeignKeys();
        for (ForeignKeyDef foreignKeyDef : foreignKeys) {
            int size = foreignKeyDef.getChildColumns().size();
            if (size > 1) {
                throw new RuntimeException("Foreign Key on more than one column are not yet supported. The foreignKey (" + foreignKeyDef.getName() + ") has " + size);
            }
            ColumnDef foreignKeyColumn = foreignKeyDef.getChildColumns().get(0);
            if (this.columnForeignKeyMap.get(foreignKeyColumn) != null) {
                throw new RuntimeException("Two foreign keys on the same column are not supported. The column (" + foreignKeyColumn.getFullyQualifiedName() + ") has more than one foreign key.");
            }
            this.columnForeignKeyMap.put(foreignKeyColumn, foreignKeyDef);
        }

        // Unique Keys
        for (UniqueKeyDef uniqueKeyDef : tableDef.getUniqueKeys()) {
            for (ColumnDef columnDef : uniqueKeyDef.getColumns()) {
                this.columnUniqueKeyMap.put(columnDef, uniqueKeyDef);
            }
        }

        return this;
    }

    public static DataGeneration of() {
        return new DataGeneration();
    }

    /**
     * @param columnDef
     * @return
     */
    public <T> DataGenerator<T> getDataGenerator(ColumnDef<T> columnDef) {
        return dataGenerators.get(columnDef);
    }

    /**
     * Function that is used to build the data generator for the column
     * It had the generators to the map of Generators.
     * <p>
     * This is a reflective function who can call itself when the generator depends on another column generator.
     * This is also a function that can create several generator for several columns (for instance, if the column is part
     * of an unique key, one generator will be created with all columns at once).
     */
    public <T> void buildDefaultDataGeneratorForColumn(ColumnDef<T> columnDef) {


        DataGenerator generator = dataGenerators.get(columnDef);
        if (generator == null) {

            // When read from a data definition file into the column property
            final Object generatorProperty = Maps.getPropertyCaseIndependent(columnDef.getProperties(), GENERATOR_PROPERTY_KEY);
            if (generatorProperty != null) {

                final Map<String, Object> generatorColumnProperties;
                try {
                    generatorColumnProperties = (Map<String, Object>) generatorProperty;
                } catch (ClassCastException e) {
                    throw new RuntimeException("The values of the property (" + GENERATOR_PROPERTY_KEY + ") for the column (" + columnDef.getFullyQualifiedName() + ") should be a map value. Bad values:" + generatorProperty);
                }

                final String nameProperty = (String) Maps.getPropertyCaseIndependent(generatorColumnProperties, "name");
                if (nameProperty == null) {
                    throw new RuntimeException("The name property of the generator was not found within the property (" + GENERATOR_PROPERTY_KEY + ") of the column " + columnDef.getFullyQualifiedName() + ".");
                }
                DataGenerator<T> dataGenerator;
                String name = nameProperty.toLowerCase();
                switch (name) {
                    case "sequence":
                        dataGenerator = SequenceGenerator.of(columnDef);
                        break;
                    case "unique":
                        dataGenerator = SequenceGenerator.of(columnDef);
                        break;
                    case "derived":
                        dataGenerator = DerivedGenerator.of(columnDef, this);
                        break;
                    case "random":
                        dataGenerator = DistributionGenerator.of(columnDef);
                        break;
                    case "distribution":
                        dataGenerator = DistributionGenerator.of(columnDef);
                        break;
                    default:
                        throw new RuntimeException("The generator (" + name + ") defined for the column (" + columnDef.getFullyQualifiedName() + ") is unknown");
                }
                dataGenerators.put(columnDef,dataGenerator);
                return;

            }
        }

        // A data generator was not yet fund, we will find one with the column constraint
        if (primaryColumns.contains(columnDef)) {

            final List<ColumnDef> primaryColumnsForColumnDefTable = primaryColumns.stream().filter(c -> c.getRelationDef().equals(columnDef.getRelationDef())).collect(Collectors.toList());
            UniqueDataGenerator uniqueDataGenerator = new UniqueDataGenerator(primaryColumnsForColumnDefTable);
            for (ColumnDef pkColumns : primaryColumnsForColumnDefTable) {
                dataGenerators.put(pkColumns, uniqueDataGenerator);
            }
            return;

        } else if (columnForeignKeyMap.keySet().contains(columnDef)) {

            final FkDataGenerator dataGenerator = new FkDataGenerator(columnForeignKeyMap.get(columnDef));
            dataGenerators.put(columnDef, dataGenerator);
            return;

        } else if (columnUniqueKeyMap.keySet().contains(columnDef)) {

            final List<ColumnDef> uniqueKeyColumns = columnUniqueKeyMap.get(columnDef).getColumns();
            UniqueDataGenerator uniqueDataGenerator = new UniqueDataGenerator(uniqueKeyColumns);
            for (ColumnDef uniqueKeyColumn : uniqueKeyColumns) {
                dataGenerators.put( uniqueKeyColumn,uniqueDataGenerator);
            }
            return;

        }

        // Else
        dataGenerators.put(columnDef,new DistributionGenerator<>(columnDef));


    }


    /**
     * This function starts the data generation and data insertion for all tables specified
     * @return the tables loaded which could be more that the tables asked if the parent loading option is on
     */
    public List<TableDef> load() {

        final List<TableDef> tablesLoaded = new ArrayList<>(tablesToLoad.keySet());

        // Parent check
        // Parent not in the table set to load ?
        // If yes, add the parent to the tables to loaded
        for (TableDef tableDef:tablesLoaded) {
            if (tableDef.getForeignKeys().size() != 0) {
                for (ForeignKeyDef foreignKeyDef : tableDef.getForeignKeys()) {
                    TableDef parentTableDef = foreignKeyDef.getForeignPrimaryKey().getTableDef();
                    if (!tablesToLoad.keySet().contains(parentTableDef)) {

                        Integer rows = Tables.getSize(parentTableDef);
                        if (rows == 0) {
                            if (this.loadParent) {
                                LOGGER.info("The table (" + parentTableDef.getFullyQualifiedName() + ") has no rows, the option to load the parent is on, therefore the table will be loaded.");
                                tablesLoaded.add(parentTableDef);
                            } else {
                                throw new RuntimeException("The table (" + tableDef.getFullyQualifiedName() + ") has a foreign key to the parent table (" + parentTableDef.getFullyQualifiedName() + "). This table has no rows and the option to load parent is disabled, we cannot then generated rows in the table (" + tableDef.getFullyQualifiedName() + ")");
                            }
                        }

                    }
                }
            }
        }

        // Load
        for (TableDef tableDef: Dag.get(tablesLoaded).getCreateOrderedTables()) {


            // The load
            LOGGER.info("Loading the table (" + tableDef.getFullyQualifiedName() + ")");
            LOGGER.info("The size of the table (" + tableDef.getFullyQualifiedName() + ") before insertion is : " + Tables.getSize(tableDef));

            // First pass to create a default generator if they are not specified
            for (ColumnDef columnDef : tableDef.getColumnDefs()) {

                if (dataGenerators.get(columnDef) == null) {
                    buildDefaultDataGeneratorForColumn(columnDef);
                }

            }

            // The number of row may be trimmed if the generator cannot generate them
            // or if there is already rows in the table
            Integer numberOfRowToInsert = getNumberOfRowToInsert(tableDef);

            if (numberOfRowToInsert > 0) {
                LOGGER.info("Inserting " + numberOfRowToInsert + " rows into the table (" + tableDef.getFullyQualifiedName() + ")");
                try (
                        SqlInsertStream inputStream = Streams.getSqlInsertStream(tableDef)
                ) {
                    for (int i = 0; i < numberOfRowToInsert; i++) {

                        Map<ColumnDef, Object> columnValues = new HashMap<>();
                        for (ColumnDef columnDef : tableDef.getColumnDefs()) {
                            populateColumnValues(columnValues, columnDef);
                        }

                        List<Object> values = new ArrayList<>();
                        for (ColumnDef columnDef : tableDef.getColumnDefs()) {
                            // We need also a recursion here to create the value
                            values.add(columnValues.get(columnDef));
                        }
                        inputStream.insert(values);
                    }
                }

            }

            LOGGER.info(numberOfRowToInsert + " records where inserted into the table (" + tableDef.getFullyQualifiedName() + ")");
            LOGGER.info("The new size is: " + Tables.getSize(tableDef));

        }
        return tablesLoaded;

    }

    /**
     * Get the max numbers of row that we can insert into the table.
     * <p>
     * * The generator has a max value of generated data
     * * and the table may have already data
     *
     * @param tableDef
     * @return
     */
    private Integer getNumberOfRowToInsert(TableDef tableDef) {


        // Precision of a sequence (Pk of unique col) make that we cannot insert the number of rows that we want
        Integer maxNumberOfRowToInsert = null;

        // Select the data generators only for this table
        final List<DataGenerator> dataGeneratorsForTable =
                dataGenerators
                        .values()
                        .stream()
                        .filter(t -> t.getColumn().getRelationDef().equals(tableDef))
                        .collect(Collectors.toList());
        for (DataGenerator dataGenerator : dataGeneratorsForTable) {
            final Integer maxGeneratedValues = (dataGenerator.getMaxGeneratedValues()).intValue();
            if (maxNumberOfRowToInsert == null) {
                maxNumberOfRowToInsert = maxGeneratedValues;
            } else {
                if (maxNumberOfRowToInsert > maxGeneratedValues) {
                    maxNumberOfRowToInsert = maxGeneratedValues;
                }
            }
        }

        final Integer totalRows = tablesToLoad.get(tableDef);
        Integer numberOfRowToInsert = totalRows;
        if (numberOfRowToInsert == null) {
            if (maxNumberOfRowToInsert < MAX_INSERT) {
                numberOfRowToInsert = maxNumberOfRowToInsert;
            } else {
                final String msg = "For the table (" + tableDef.getFullyQualifiedName() + "), the total number of rows to insert is not defined and the max number of rows is " + maxNumberOfRowToInsert + " greater than the allowed max " + MAX_INSERT + ". Set a number of rows to insert.";
                LOGGER.severe(msg);
                throw new RuntimeException(msg);
            }
        }

        if (maxNumberOfRowToInsert < numberOfRowToInsert) {
            final String msg = "For the table (" + tableDef.getFullyQualifiedName() + "), the max number of rows is " + maxNumberOfRowToInsert + " not " + totalRows;
            LOGGER.severe(msg);
            throw new RuntimeException(msg);
        }

        Integer numberOfRows = Tables.getSize(tableDef);
        if (numberOfRows != 0) {
            numberOfRowToInsert = numberOfRowToInsert - numberOfRows;
            if (numberOfRowToInsert <= 0) {
                LOGGER.warning("The table (" + tableDef.getFullyQualifiedName() + ") can not accept any more rows");
                numberOfRowToInsert = 0;
            }
        }

        return numberOfRowToInsert;

    }

    public DataGeneration loadParentTable(Boolean loadParent) {
        this.loadParent = loadParent;
        return this;
    }

    /**
     * Recursive function that create data for each column for a row
     * The function is recursive to be able to handle direct relationship between columns (ie derived generator)
     *
     * @param columnDef
     * @param columnValues
     */
    private void populateColumnValues(Map<ColumnDef, Object> columnValues, ColumnDef columnDef) {

        if (columnValues.get(columnDef) == null) {

            DataGenerator dataGenerator = dataGenerators.get(columnDef);

            if (dataGenerator.getClass().equals(DerivedGenerator.class)) {
                DerivedGenerator dataGeneratorDerived = (DerivedGenerator) dataGenerator;
                ColumnDef parentColumn = dataGeneratorDerived.getParentGenerator().getColumn();
                // The column value of the parent must be generated before
                populateColumnValues(columnValues, parentColumn);
            }
            if (dataGenerator.getColumns().size() == 1) {
                columnValues.put(columnDef, dataGenerator.getNewValue());
            } else {
                columnValues.put(columnDef, dataGenerator.getNewValue(columnDef));
            }

        }

    }


    public <T> DataGeneration addGenerator(DataGenerator<T> dataGenerator) {
        dataGenerators.put(dataGenerator.getColumn(), dataGenerator);
        return this;
    }

    /**
     * An utiliy given to the generator to extract the data generator properties
     * @param columnDef
     * @return - the data generation properties or null
     */
    public static <T> Map<String, Object> getProperties(ColumnDef<T> columnDef) {
        Map<String, Object> properties = columnDef.getProperties();
        final Object generatorProperty = Maps.getPropertyCaseIndependent(properties, GENERATOR_PROPERTY_KEY);
        Map<String, Object> generatorColumnProperties = null;
        if (generatorProperty != null) {
            try {
                generatorColumnProperties = (Map<String, Object>) generatorProperty;
            } catch (ClassCastException e) {
                throw new RuntimeException("The values of the property (" + GENERATOR_PROPERTY_KEY + ") for the column (" + columnDef.getFullyQualifiedName() + ") should be a map value. Bad values:" + generatorProperty);
            }
        }
        return generatorColumnProperties;
    }

    public DataGeneration addTable(TableDef tableDef) {
        return addTable(tableDef,null);
    }

    public DataGeneration addTables(List<TableDef> tableDefs) {
        for (TableDef tableDef:tableDefs){
            addTable(tableDef);
        }
        return this;
    }

    /**
     *
     * @param tableDefs
     * @param totalRows - the totalRows
     * @return
     */
    public DataGeneration addTables(List<TableDef> tableDefs, Integer totalRows) {
        for (TableDef tableDef:tableDefs){
            addTable(tableDef,totalRows);
        }
        return this;
    }
}
