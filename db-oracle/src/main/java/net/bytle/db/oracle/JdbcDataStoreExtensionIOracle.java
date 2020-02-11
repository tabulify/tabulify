package net.bytle.db.oracle;

import net.bytle.db.database.DataTypeDatabase;
import net.bytle.db.jdbc.JdbcDataSystemSql;
import net.bytle.db.jdbc.JdbcDataPath;
import net.bytle.db.jdbc.JdbcDataSystem;
import net.bytle.db.spi.DataPath;
import oracle.jdbc.OracleTypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by gerard on 28-11-2015.
 */
public class JdbcDataStoreExtensionIOracle extends JdbcDataStoreExtension {

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

    public JdbcDataStoreExtensionIOracle(JdbcDataSystem jdbcDataSystem) {
        super(jdbcDataSystem);
    }


    @Override
    public DataTypeDatabase dataTypeOf(Integer typeCode) {
        return dataTypeDatabaseSet.get(typeCode);
    }

    /**
     * Returns statement to create the table
     *
     * @param dataPath
     * @return
     */
    @Override
    public List<String> getCreateTableStatements(JdbcDataPath dataPath) {
        return null;
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

    @Override
    public String getTruncateStatement(DataPath dataPath) {
        StringBuilder truncateStatementBuilder = new StringBuilder().append("truncate from ");
        truncateStatementBuilder.append(JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath));
        return truncateStatementBuilder.toString();
    }


}
