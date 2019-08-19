package net.bytle.db.gen;

import net.bytle.cli.Log;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.*;
import net.bytle.db.stream.SqlInsertStream;
import net.bytle.db.stream.Streams;
import net.bytle.type.Strings;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataGenLoader {


    private static final Log LOGGER = Gen.GEN_LOG;
    private final TableDef tableDef;
    private final DataGenDef dataGenDef;
//    private Map<String, Map<String, Object>> generatorDefinition = new HashMap<>();
    private final Map<ColumnDef, ForeignKeyDef> columnForeignKeyMap = new HashMap<>();
    private final Map<ColumnDef, UniqueKeyDef> columnUniqueKeyMap = new HashMap<>();
    private List<ColumnDef> primaryColumns = new ArrayList<>();

    // A structure to hold the data generator by column
    Map<ColumnDef, DataGenerator> dataGenerators = new HashMap<>();

    private DataGenLoader(DataGenDef dataGenDef) {

        this.dataGenDef = dataGenDef;
        this.tableDef = dataGenDef.getTableDef();

        // Self referencing foreign key check
        List<ForeignKeyDef> selfReferencingForeignKeys = DataGens.getSelfReferencingForeignKeys(this.tableDef);
        if (selfReferencingForeignKeys.size() > 0) {
            for (ForeignKeyDef foreignKeyDef : selfReferencingForeignKeys) {
                LOGGER.severe("The foreign key " + foreignKeyDef.getName() + " on the table (" + foreignKeyDef.getTableDef().getFullyQualifiedName() + ") references itself and it's not supported.");
            }
            throw new RuntimeException("Self referencing foreign key found int he table " + this.tableDef.getFullyQualifiedName());
        }


        // Primary Key with only one column are supported
        PrimaryKeyDef primaryKeyDef = this.tableDef.getPrimaryKey();
        // Extract the primary column
        if (primaryKeyDef != null) {
            primaryColumns = primaryKeyDef.getColumns();
        }

        // Foreign Key with only one column are supported
        List<ForeignKeyDef> foreignKeys = this.tableDef.getForeignKeys();
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
        for (UniqueKeyDef uniqueKeyDef : this.tableDef.getUniqueKeys()) {
            for (ColumnDef columnDef : uniqueKeyDef.getColumns()) {
                this.columnUniqueKeyMap.put(columnDef, uniqueKeyDef);
            }
        }

    }




    public void load() {


        // The load
        LOGGER.info("Loading the table (" + tableDef.getFullyQualifiedName() + ")");
        LOGGER.info("The size of the table (" + tableDef.getFullyQualifiedName() + ") before insertion is : " + Tables.getSize(tableDef));

        // First pass to create the generator map
        for (DataGenDefColumnDef dataGenColumnDef : dataGenDef.getDataGenColumnDefs()) {

            buildDataGeneratorForColumn(dataGenColumnDef);

        }

        // The number of row may be trimmed if the generator cannot generate them
        // or if there is already rows in the table
        Integer numberOfRowToInsert = getNumberOfRowToInsert(dataGenDef.getRows());

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

    /**
     * Get the max numbers of row that we can insert into the table.
     *
     *    * The generator has a max value of generated data
     *    * and the table may have already data
     *
     * @param rows
     * @return
     */
    private Integer getNumberOfRowToInsert(Integer rows) {

        Integer numberOfRowToInsert = rows;

        // Precision of a sequence (Pk of unique col) make that we cannot insert the number of rows that we want
        Integer maxNumberOfRowToInsert = null;
        for (DataGenerator dataGenerator : dataGenerators.values()) {
            final Integer maxGeneratedValues = (dataGenerator.getMaxGeneratedValues()).intValue();
            if (maxNumberOfRowToInsert==null){
                maxNumberOfRowToInsert=maxGeneratedValues;
            } else {
                if (maxNumberOfRowToInsert > maxGeneratedValues) {
                    maxNumberOfRowToInsert = maxGeneratedValues;
                }
            }
        }

        if (maxNumberOfRowToInsert < numberOfRowToInsert) {
            numberOfRowToInsert = maxNumberOfRowToInsert;
            LOGGER.warning("For the table (" + tableDef.getFullyQualifiedName() + "), the max number of rows is " + maxNumberOfRowToInsert + " not " + rows);
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

    /**
     * Function that is used to build the data generator for the column
     * It had the generators to the map of Generators.
     * <p>
     * This is a reflective function who can call itself when the generator depends on another column generator.
     * This is also a function that can create several generator for several columns (for instance, if the column is part
     * of an unique key, one generator will be created with all columns at once).
     *
     * @param dataGenColumnDef
     */
    private void buildDataGeneratorForColumn(DataGenDefColumnDef dataGenColumnDef) {

        // Because of the recursion, the generator has may be already be made
        if (dataGenerators.get(dataGenColumnDef) == null) {

            String generator = dataGenColumnDef.getGeneratorName();
            if (generator != null) {
                try {

                    if (generator.equals("sequence") || generator.equals("unique")) {

                        SequenceGenerator dataGenerator = new SequenceGenerator(dataGenColumnDef.getColumnDef());

                        final Integer step = (Integer) dataGenColumnDef.getProperty("step");
                        if (step != null) {
                            dataGenerator.step(step);
                        }

                        final List<String> values = (List<String>) dataGenColumnDef.getProperty("values");
                        if (values != null) {
                            dataGenerator.values(values);
                        }

                        dataGenerators.put(dataGenColumnDef.getColumnDef(), dataGenerator);
                        return;


                    } else if (generator.equals("derived")) {

                        Class[] constructorClassType = {ColumnDef.class, DataGenerator.class, String.class};

                        // Get DerivedGenerator Class Constructor
                        String classGeneratorName = Strings.toCamelCase(generator);
                        Class<?> generatorClass = Class.forName(DataGenLoader.class.getPackage().getName() + "." + classGeneratorName + "Generator");
                        Constructor constructor = generatorClass.getConstructor(constructorClassType);

                        // Parent Generator
                        final String columnParentKeyProperty = "column_parent";
                        String columnName = (String) dataGenColumnDef.getProperty(columnParentKeyProperty);
                        if (columnName == null) {
                            throw new IllegalArgumentException("The parent column is not defined in the '" + columnParentKeyProperty + "' properties for the column " + dataGenColumnDef.getColumnDef().getFullyQualifiedName());
                        }
                        ColumnDef columnParent = dataGenColumnDef.getColumnDef().getRelationDef().getColumnOf(columnName);
                        DataGenerator parentGenerator = dataGenerators.get(columnParent);
                        if (parentGenerator == null) {
                            if (dataGenColumnDef.getColumnDef().equals(columnParent)) {
                                throw new RuntimeException("The column (" + dataGenColumnDef.getColumnDef().getFullyQualifiedName() + " has a derived generator and derived from itself creating a loop. Please choose another column as derived (parent) column.");
                            }
                            buildDataGeneratorForColumn(DataGenDefColumnDef.get(columnParent));
                        }
                        parentGenerator = dataGenerators.get(columnParent);

                        // Formula
                        String formula = (String) dataGenColumnDef.getProperty("formula");
                        Object[] constructorParamValue = {dataGenColumnDef.getColumnDef(), parentGenerator, formula};

                        // New Instance
                        DataGenerator dataGenerator = (DataGenerator) constructor.newInstance(constructorParamValue);
                        dataGenerators.put(dataGenColumnDef.getColumnDef(), dataGenerator);
                        return;


                    } else if (generator.equals("random") || generator.equals("distribution") ) {

                        final DistributionGenerator distributionGenerator = new DistributionGenerator(dataGenColumnDef.getColumnDef());
                        dataGenerators.put(dataGenColumnDef.getColumnDef(), distributionGenerator);

                        Map<Object,Integer> buckets = (Map<Object, Integer>) dataGenColumnDef.getProperty("buckets");

                        // DataType Check
                        if (buckets!=null) {
                            Object o = buckets.entrySet().iterator().next().getKey();
                            if (o.getClass() != dataGenColumnDef.getColumnDef().getDataType().getClazz()) {
                                throw new RuntimeException("The data type of the key with the the value (" + o + ") in the buckets definition of the column " + dataGenColumnDef.getColumnDef().getFullyQualifiedName() + " is not a " + dataGenColumnDef.getColumnDef().getDataType().getClazz().getSimpleName() + " but a " + o.getClass().getSimpleName() + ".");
                            }
                            distributionGenerator.setBuckets(buckets);
                        }

                        return;

                    } else {

                        throw new RuntimeException("The generator properties (" + generator + ") is unknown for the column (" + dataGenColumnDef.getColumnDef().getFullyQualifiedName() + ")");

                    }

                } catch (Exception e) {

                    LOGGER.severe("Error for the column "+dataGenColumnDef.getColumnDef().getFullyQualifiedName());
                    throw new RuntimeException(e);

                }

            }

            // A data generator was not yet fund, we will find one with the column constraint
            if (primaryColumns.contains(dataGenColumnDef.getColumnDef())) {

                UniqueDataGenerator uniqueDataGenerator = new UniqueDataGenerator(primaryColumns);
                for (ColumnDef pkColumns : primaryColumns) {
                    dataGenerators.put(pkColumns, uniqueDataGenerator);
                }
                return;

            } else if (columnForeignKeyMap.keySet().contains(dataGenColumnDef.getColumnDef())) {

                dataGenerators.put(dataGenColumnDef.getColumnDef(), new FkDataGenerator(columnForeignKeyMap.get(dataGenColumnDef.getColumnDef())));
                return;

            } else if (columnUniqueKeyMap.keySet().contains(dataGenColumnDef.getColumnDef())) {

                final List<ColumnDef> uniqueKeyColumns = columnUniqueKeyMap.get(dataGenColumnDef.getColumnDef()).getColumns();
                UniqueDataGenerator uniqueDataGenerator = new UniqueDataGenerator(uniqueKeyColumns);
                for (ColumnDef uniqueKeyColumn : uniqueKeyColumns) {
                    dataGenerators.put(uniqueKeyColumn, uniqueDataGenerator);
                }
                return;

            }

            // Else
            dataGenerators.put(dataGenColumnDef.getColumnDef(), new DistributionGenerator(dataGenColumnDef.getColumnDef()));

        }
    }


    /**
     * Return a dataGenLoader
     *
     * @param dataGenDef
     * @return
     */
    public static DataGenLoader get(DataGenDef dataGenDef) {
        return new DataGenLoader(dataGenDef);
    }


}
