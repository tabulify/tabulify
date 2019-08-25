package net.bytle.db.gen;


import net.bytle.db.engine.Columns;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.DataType;

import java.sql.Date;
import java.util.*;


public class UniqueDataGenerator implements DataGenerator {



    private Map<ColumnDef,DataGenerator> dataGeneratorMap = new HashMap<>();

    Integer position = new Integer(0);

    /**
     * Get the max value of a primary key numeric column and add 1 to the value each time
     * the {@link #getNewValue()} is called
     * @param columnDefs
     */
    public UniqueDataGenerator(List<ColumnDef> columnDefs) {

        // long numberOfValueToGenerateByColumn = Math.floorDiv((long) numberOfRowToInsert,(long) columnDefs.size());

        // Creating a data generator by column
        // and adding it to the data generator map variable
        for(ColumnDef columnDef: columnDefs) {

            if (DataType.timeTypes.contains(columnDef.getDataType().getTypeCode())) {

                // With date, we are going in the past
                ColumnDef<Date> dateColumn = Columns.safeCast(columnDef,Date.class);
                Date minDate = Tables.getMin(dateColumn);
                dataGeneratorMap.put(columnDef,SequenceGenerator.of(dateColumn).start(minDate).step(-1));


            } else if (DataType.numericTypes.contains(columnDef.getDataType().getTypeCode())) {

                ColumnDef<Integer> integerColumn = Columns.safeCast(columnDef,Integer.class);
                Integer intCounter = Tables.getMax(integerColumn);
                dataGeneratorMap.put(columnDef,SequenceGenerator.of(integerColumn).start(intCounter).step(1));

            } else if ( DataType.characterTypes.contains(columnDef.getDataType().getTypeCode())) {

                ColumnDef<String> stringColumn = Columns.safeCast(columnDef,String.class);
                String s = Tables.getMax(stringColumn);
                dataGeneratorMap.put(columnDef,SequenceGenerator.of(stringColumn).start(s));

            } else {

                throw new RuntimeException("The data type (" + columnDef.getDataType().getTypeCode() + "," + columnDef.getDataType().getTypeName() + ") is not yet implemented for the column " + columnDef.getFullyQualifiedName() + ").");

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
    public ColumnDef getColumn() {

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


        final DataGenerator dataGenerator = dataGeneratorMap.get(columnDef);
        return dataGenerator.getNewValue(columnDef);

    }

    /**
     * get the actual value of a column
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
    public Double getMaxGeneratedValues() {
        // Hack
        return Double.valueOf(Integer.MAX_VALUE);
    }
}
