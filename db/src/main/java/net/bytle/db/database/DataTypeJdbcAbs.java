package net.bytle.db.database;

/**
 * Created by gerard on 28-11-2015.
 * A default implementation
 */
public abstract class DataTypeJdbcAbs implements DataTypeJdbc {



    @Override
    public String toString() {
        return "DataTypeJdbc{ TypeCode:"+getTypeCode()+",TypeName:"+getTypeName()+"}";
    }

}
