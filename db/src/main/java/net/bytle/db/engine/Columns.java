package net.bytle.db.engine;

import net.bytle.db.model.ColumnDef;


/**
 * Static utility function for columns
 */

public class Columns {


    @SuppressWarnings("unchecked")
    public static <T> ColumnDef<T> safeCast(ColumnDef columnDef, Class<T> clazz) {
        if (columnDef.getClazz().equals(clazz)){
            return (ColumnDef<T>) columnDef;
        } else {
            throw new RuntimeException("The class of the column ("+columnDef.getColumnName()+") is " + columnDef.getClazz() + " and not" + clazz);
        }
    }

    public static <T> T getMin(ColumnDef<T> columnDef){
        return columnDef.getRelationDef().getDataPath().getDataSystem().getProcessingEngine().getMin(columnDef);
    }

    public static <T> T getMax(ColumnDef<T> columnDef) {
        return columnDef.getRelationDef().getDataPath().getDataSystem().getProcessingEngine().getMax(columnDef);
    }

}
