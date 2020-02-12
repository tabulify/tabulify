package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 *
 */
public class DecimalSql extends SqlDataType {

    protected static final int TYPE_CODE = Types.DECIMAL;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "DECIMAL";
    }

    @Override
    public Class<?> getClazz() {
        return java.math.BigDecimal.class;
    }


}
