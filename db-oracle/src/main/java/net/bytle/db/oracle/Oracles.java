package net.bytle.db.oracle;

import java.sql.Connection;

public class Oracles {

    public static Object castLoadObjectIfNecessary(Connection targetConnection, int targetColumnType, Object sourceObject) {

            if (targetColumnType == OracleTypes.BINARY_DOUBLE && sourceObject instanceof Double) {
                return new oracle.sql.BINARY_DOUBLE((Double) sourceObject);
            } else if (targetColumnType == OracleTypes.BINARY_FLOAT && sourceObject instanceof Float) {
                return new oracle.sql.BINARY_FLOAT((Float) sourceObject);
            } else {
                return sourceObject;
            }

    }
}
