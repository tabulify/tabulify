package net.bytle.db.gen;

import net.bytle.db.model.ColumnDef;
import net.bytle.type.Maps;


import java.util.Map;

/**
 * A wrapper around a columnDef that will build and link the data generator
 */
public class DataGenDefColumnDef<T> {

    private final ColumnDef<T> columnDef;



    /**
     * The data generator
     */
    private DataGenerator dataGenerator;

    /**
     *
     *
     * @param columnDef
     */
    private DataGenDefColumnDef(DataGenDef dataGenDef, ColumnDef<T> columnDef) {

        this.columnDef = columnDef;



    }

    public static <T> DataGenDefColumnDef<T> of(DataGenDef dataGenDef, ColumnDef<T> columnDef) {
        return new DataGenDefColumnDef<>(dataGenDef, columnDef);
    }




    public ColumnDef<T> getColumnDef() {
        return columnDef;
    }


    public DataGenDefColumnDef setGenerator(DataGenerator dataGenerator) {
        this.dataGenerator = dataGenerator;
        return this;
    }

    public DataGenerator getDataGenerator() {
        return dataGenerator;
    }
}
