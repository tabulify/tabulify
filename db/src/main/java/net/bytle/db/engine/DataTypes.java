package net.bytle.db.engine;

import net.bytle.db.database.JdbcDataType.DataTypesJdbc;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;

public class DataTypes {


    final static List<Integer> NUMERICS = Arrays.asList(Types.INTEGER, Types.DECIMAL, Types.NUMERIC, Types.DOUBLE);

    public static boolean isNumeric(int typeCode) {
        return NUMERICS.contains(typeCode);
    }

    /**
     * @param typeName
     * @return the jdbc integer representation of the type name
     */
    public static Integer toInteger(String typeName) {
        return DataTypesJdbc.of(typeName).getTypeCode();
    }
}
