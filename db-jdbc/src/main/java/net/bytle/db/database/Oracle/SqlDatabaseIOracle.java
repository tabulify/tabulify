package net.bytle.db.database.Oracle;

import net.bytle.db.database.DataTypeDatabase;
import net.bytle.db.database.Database;
import net.bytle.db.database.SqlDatabase;
import net.bytle.db.jdbc.JdbcDataSystem;
import oracle.jdbc.OracleTypes;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by gerard on 28-11-2015.
 */
public class SqlDatabaseIOracle extends SqlDatabase {

    private static Map<Integer, DataTypeDatabase> dataTypeDatabaseSet = new HashMap<Integer,DataTypeDatabase>();

    static {
        dataTypeDatabaseSet.put(OraDbDoubleType.TYPE_CODE, new OraDbDoubleType());
        dataTypeDatabaseSet.put(OraDbIntervalDsType.TYPE_CODE, new OraDbIntervalDsType());
        dataTypeDatabaseSet.put(OraDbIntervalYmType.TYPE_CODE, new OraDbIntervalYmType());
        dataTypeDatabaseSet.put(OraDbRawType.TYPE_CODE, new OraDbRawType());
        dataTypeDatabaseSet.put(OraDbNumberType.TYPE_CODE, new OraDbNumberType());
        dataTypeDatabaseSet.put(OraDbLongType.TYPE_CODE, new OraDbLongType());
        dataTypeDatabaseSet.put(OraDbNVarchar2Type.TYPE_CODE, new OraDbNVarchar2Type());
        dataTypeDatabaseSet.put(OraDbLongRawType.TYPE_CODE, new OraDbLongRawType());
    }

    public SqlDatabaseIOracle(JdbcDataSystem jdbcDataSystem) {
        super(jdbcDataSystem);
    }


    @Override
    public DataTypeDatabase dataTypeOf(Integer typeCode) {
        return dataTypeDatabaseSet.get(typeCode);
    }


    @Override
    public Object getLoadObject(int targetColumnType, Object sourceObject) {

        if (targetColumnType == OracleTypes.BINARY_DOUBLE && sourceObject instanceof Double) {
            return new oracle.sql.BINARY_DOUBLE((Double) sourceObject);
        } else if (targetColumnType == OracleTypes.BINARY_FLOAT && sourceObject instanceof Float) {
            return new oracle.sql.BINARY_FLOAT((Float) sourceObject);
        } else {
            return sourceObject;
        }

    }

    @Override
    public Integer getMaxWriterConnection() {
        return 100;
    }


}
