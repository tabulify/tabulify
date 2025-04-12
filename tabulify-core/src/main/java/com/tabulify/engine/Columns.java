package com.tabulify.engine;

import com.tabulify.model.ColumnDef;
import net.bytle.type.Casts;


/**
 * Static utility function for columns
 */

public class Columns {



    public static <T> T getMin(Class<T> clazz, ColumnDef columnDef){
      Object min = columnDef.getRelationDef().getDataPath().getConnection().getProcessingEngine().getMin(columnDef);
      return Casts.castSafe(min, clazz);
    }

    public static <T> T getMax(Class<T> clazz, ColumnDef columnDef) {
      Object max = columnDef.getRelationDef().getDataPath().getConnection().getProcessingEngine().getMax(columnDef);
      return Casts.castSafe(max, clazz);
    }

}
