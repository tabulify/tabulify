package net.bytle.db.database.Hive;

import net.bytle.db.database.DataTypeDatabase;
import net.bytle.db.database.SqlDatabase;
import net.bytle.db.jdbc.JdbcDataSystem;
import net.bytle.db.model.TableDef;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by gerard on 11-01-2016.
 */
public class SqlDatabaseIHive extends SqlDatabase {


    private static Map<Integer, DataTypeDatabase> dataTypeDatabaseSet = new HashMap<>();

    static {
        dataTypeDatabaseSet.put(HiveIntegerType.TYPE_CODE, new HiveIntegerType());
        dataTypeDatabaseSet.put(HiveCharType.TYPE_CODE, new HiveCharType());
        dataTypeDatabaseSet.put(HiveVarCharType.TYPE_CODE, new HiveVarCharType());
        dataTypeDatabaseSet.put(HiveTimeType.TYPE_CODE, new HiveTimeType());
        dataTypeDatabaseSet.put(HiveNumericType.TYPE_CODE, new HiveNumericType());
        dataTypeDatabaseSet.put(HiveSmallIntType.TYPE_CODE, new HiveSmallIntType());
    }

    public SqlDatabaseIHive(JdbcDataSystem jdbcDataSystem) {
        super(jdbcDataSystem);
    }


    @Override
    public DataTypeDatabase dataTypeOf(Integer typeCode) {
        return dataTypeDatabaseSet.get(typeCode);
    }

    @Override
    public List<String> getCreateTableStatements(TableDef tableDef, String name) {
        return null;
    }


    @Override
    public Object getLoadObject(int targetColumnType, Object sourceObject) {
        return null;
    }

    @Override
    public String getNormativeSchemaObjectName(String objectName) {
        return null;
    }

    @Override
    public Integer getMaxWriterConnection() {
        // The JDBCMetadata().getMaxConnections() method returns a Method Not Supported exception
        return 5;
    }

}
