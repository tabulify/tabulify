package net.bytle.db.database;

import net.bytle.db.model.DataTypeInterface;

/**
 * Created by gerard on 4-5-2015.
 *
 * The abstract class for the DataTypeDatabase Interface
 */
public abstract class DataTypeDatabaseAbs implements DataTypeDatabase, DataTypeInterface {


    @Override
    public String getCreateStatement(int precision, int scale) {
        return null;
    }

    @Override
    public String toString() {
        return "DataTypeDatabase{ TypeCode:"+getTypeCode()+",TypeName:"+getTypeName()+"}";
    }

}
