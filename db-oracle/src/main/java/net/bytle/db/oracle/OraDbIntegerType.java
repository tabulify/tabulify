package net.bytle.db.oracle;

import net.bytle.db.database.DataTypeDatabaseAbs;

import java.sql.Types;

/**
 * Created by gerard on 28-11-2015.
 */
class OraDbIntegerType extends DataTypeDatabaseAbs {

    static Integer TYPE_CODE = Types.INTEGER;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "INTEGER";
    }

  @Override
  public String getCreateStatement(int precision, int scale) {
    return getTypeName();
  }

  @Override
    public Class<?> getJavaDataType() {
        return Integer.class;
    }

    @Override
    public Class<?> getVendorClass() {
        return Integer.class;
    }

}
