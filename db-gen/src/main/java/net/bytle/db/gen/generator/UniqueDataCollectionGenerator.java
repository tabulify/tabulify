package net.bytle.db.gen.generator;


import net.bytle.db.engine.Columns;
import net.bytle.db.gen.GenColumnDef;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.SqlDataType;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.*;


public class UniqueDataCollectionGenerator implements CollectionGenerator {



    private Map<GenColumnDef, CollectionGenerator> dataGeneratorMap = new HashMap<>();

    Integer position = new Integer(0);

    /**
     * Get the max value of a primary key numeric column and add 1 to the value each time
     * the {@link #getNewValue()} is called
     * @param columnDefs
     */
    public UniqueDataCollectionGenerator(List<GenColumnDef> columnDefs) {

        // long numberOfValueToGenerateByColumn = Math.floorDiv((long) numberOfRowToInsert,(long) columnDefs.size());

        // Creating a data generator by column
        // and adding it to the data generator map variable
        for(GenColumnDef columnDef: columnDefs) {

            if (SqlDataType.timeTypes.contains(columnDef.getDataType().getTypeCode())) {

                // With date, we are going in the past
                GenColumnDef<Date> dateColumn = (GenColumnDef<Date>) Columns.safeCast(columnDef,Date.class);
                Date minDate = Columns.getMin(dateColumn);
                dataGeneratorMap.put(columnDef, SequenceCollectionGenerator.of(dateColumn).start(minDate).step(-1));

            } else if (SqlDataType.numericTypes.contains(columnDef.getDataType().getTypeCode())) {

                if (columnDef.getClazz()== BigDecimal.class){
                    GenColumnDef<BigDecimal> bigDecimalColumnDef = (GenColumnDef<BigDecimal>) Columns.safeCast(columnDef, BigDecimal.class);
                    BigDecimal intCounter = Columns.getMax(bigDecimalColumnDef);
                    dataGeneratorMap.put(columnDef, SequenceCollectionGenerator.of(bigDecimalColumnDef).start(intCounter).step(1));
                } else {
                    GenColumnDef<Integer> integerColumn = (GenColumnDef<Integer>) Columns.safeCast(columnDef, Integer.class);
                    Integer intCounter = Columns.getMax(integerColumn);
                    dataGeneratorMap.put(columnDef, SequenceCollectionGenerator.of(integerColumn).start(intCounter).step(1));
                }

            } else if ( SqlDataType.characterTypes.contains(columnDef.getDataType().getTypeCode())) {

                GenColumnDef<String> stringColumn = (GenColumnDef<String>) Columns.safeCast(columnDef,String.class);
                String s = Columns.getMax(stringColumn);
                dataGeneratorMap.put(columnDef, SequenceCollectionGenerator.of(stringColumn).start(s));

            } else {

                throw new RuntimeException("The data type (" + columnDef.getDataType().getTypeCode() + "," + columnDef.getDataType().getTypeNames() + ") is not yet implemented for the column " + columnDef.getFullyQualifiedName() + ").");

            }

        }


    }

    /**
     * @return a new generated data object every time it's called
     */
    @Override
    public Object getNewValue() {

        // Only one column ?
        if (dataGeneratorMap.size()==1){

            ColumnDef columnDef = dataGeneratorMap.keySet().iterator().next();
            return dataGeneratorMap.get(columnDef).getNewValue();

        } else {

            throw new RuntimeException("This is a multi-column generator of "+dataGeneratorMap.size()+" columns, you should use the function getNewValue(ColumnDef)");

        }

    }

    /**
     * @return a generated value (used in case of derived data
     */
    @Override
    public Object getActualValue() {

        if (dataGeneratorMap.size()==1){

            ColumnDef columnDef = dataGeneratorMap.keySet().iterator().next();
            return dataGeneratorMap.get(columnDef).getActualValue();

        } else {

            throw new RuntimeException("This is a multi-column generator of "+dataGeneratorMap.size()+" columns, you should use the function getActualValue(ColumnDef)");

        }


    }

    /**
     * @return the column attached to this generator
     * It permits to create parent relationship between generators
     * when asking a value for a column, we may need to ask the value for another column before
     */
    @Override
    public GenColumnDef getColumn() {

        if (dataGeneratorMap.size()==1){

            return dataGeneratorMap.keySet().iterator().next();

        } else {

            throw new RuntimeException("This is a multi-column generator of "+dataGeneratorMap.size()+" columns, you should use the function getColumns()");

        }

    }

    /**
     * Get a new value for a column
     *
     * This generator is row based. You need to call each column
     * in order to have a unique set
     *
     * @param columnDef
     * @return a new generated data object every time it's called
     */
    @Override
    public Object getNewValue(ColumnDef columnDef) {


        final CollectionGenerator dataCollectionGenerator = dataGeneratorMap.get(columnDef);
        return dataCollectionGenerator.getNewValue(columnDef);

    }

    /**
     * of the actual value of a column
     *
     * @param columnDef
     * @return a generated value (used in case of derived data
     */
    @Override
    public Object getActualValue(ColumnDef columnDef) {

        return dataGeneratorMap.get(columnDef).getActualValue(columnDef);
    }

    /**
     * @return the columns attached to this generator
     */
    @Override
    public List<ColumnDef> getColumns() {
        List<ColumnDef> columnDefs = new ArrayList<>(dataGeneratorMap.keySet());
        Collections.sort(columnDefs);
        return columnDefs;
    }

    @Override
    public Long getMaxGeneratedValues() {
        // Hack
        return Long.MAX_VALUE;
    }

    @Override
    public String toString() {
        return "UniqueDataGenerator{" + dataGeneratorMap + '}';
    }
}
