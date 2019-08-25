package net.bytle.db.engine;

import net.bytle.db.model.ColumnDef;

import java.math.BigDecimal;


public class Columns {

    public static <T> ColumnDef<T> safeCast(ColumnDef columnDef, Class<T> clazz) {
        if (columnDef.getClazz().equals(clazz)){
            return (ColumnDef<T>) columnDef;
        } else {
            throw new RuntimeException("The class of the column is " + columnDef.getClazz() + " and not" + clazz);
        }
    }
}
