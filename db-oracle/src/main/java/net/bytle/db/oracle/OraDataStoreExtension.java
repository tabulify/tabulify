package net.bytle.db.oracle;

import net.bytle.db.database.DataTypeDatabase;
import net.bytle.db.jdbc.JdbcDataPath;
import net.bytle.db.jdbc.JdbcDataStore;
import net.bytle.db.jdbc.JdbcDataStoreExtension;
import net.bytle.db.jdbc.JdbcDataSystemSql;
import oracle.jdbc.OracleTypes;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by gerard on 28-11-2015.
 */
public class OraDataStoreExtension extends JdbcDataStoreExtension {

  private static Map<Integer, DataTypeDatabase> dataTypeDatabaseSet = new HashMap<Integer, DataTypeDatabase>();

  static {
    // Numeric: https://docs.oracle.com/cd/B28359_01/server.111/b28318/datatype.htm#CNCPT313
    dataTypeDatabaseSet.put(Types.DOUBLE, new OraDbNumberType());
    dataTypeDatabaseSet.put(Types.NUMERIC, new OraDbNumberType());
    dataTypeDatabaseSet.put(OraDbIntervalDsType.TYPE_CODE, new OraDbIntervalDsType());
    dataTypeDatabaseSet.put(OraDbIntervalYmType.TYPE_CODE, new OraDbIntervalYmType());
    dataTypeDatabaseSet.put(OraDbRawType.TYPE_CODE, new OraDbRawType());
    dataTypeDatabaseSet.put(OraDbLongType.TYPE_CODE, new OraDbLongType());
    dataTypeDatabaseSet.put(OraDbNVarchar2Type.TYPE_CODE, new OraDbNVarchar2Type());
    dataTypeDatabaseSet.put(OraDbLongRawType.TYPE_CODE, new OraDbLongRawType());
    dataTypeDatabaseSet.put(OraDbIntegerType.TYPE_CODE, new OraDbIntegerType());
  }

  public OraDataStoreExtension(JdbcDataStore jdbcDataStore) {
    super(jdbcDataStore);
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
  public String getTruncateStatement(JdbcDataPath dataPath) {
    return "truncate from " +
      JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath);
  }



}
