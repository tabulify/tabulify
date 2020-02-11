package net.bytle.db.jdbc.Hive;

import net.bytle.db.database.DataTypeDatabase;
import net.bytle.db.jdbc.JdbcDataPath;
import net.bytle.db.jdbc.JdbcDataStore;
import net.bytle.db.jdbc.JdbcDataStoreExtension;
import net.bytle.db.jdbc.JdbcDataSystemSql;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by gerard on 11-01-2016.
 */
public class JdbcDataStoreExtensionIHive extends JdbcDataStoreExtension {


    private static Map<Integer, DataTypeDatabase> dataTypeDatabaseSet = new HashMap<>();

    static {
        dataTypeDatabaseSet.put(HiveIntegerType.TYPE_CODE, new HiveIntegerType());
        dataTypeDatabaseSet.put(HiveCharType.TYPE_CODE, new HiveCharType());
        dataTypeDatabaseSet.put(HiveVarCharType.TYPE_CODE, new HiveVarCharType());
        dataTypeDatabaseSet.put(HiveTimeType.TYPE_CODE, new HiveTimeType());
        dataTypeDatabaseSet.put(HiveNumericType.TYPE_CODE, new HiveNumericType());
        dataTypeDatabaseSet.put(HiveSmallIntType.TYPE_CODE, new HiveSmallIntType());
    }

  public JdbcDataStoreExtensionIHive(JdbcDataStore jdbcDataStore) {
      super(jdbcDataStore);
  }


  @Override
    public DataTypeDatabase dataTypeOf(Integer typeCode) {
        return dataTypeDatabaseSet.get(typeCode);
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

    @Override
    public String getTruncateStatement(JdbcDataPath dataPath) {
        StringBuilder truncateStatementBuilder = new StringBuilder().append("truncate from ");
        truncateStatementBuilder.append(JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath));
        return truncateStatementBuilder.toString();
    }

}
