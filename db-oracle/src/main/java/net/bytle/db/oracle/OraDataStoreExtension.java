package net.bytle.db.oracle;

import net.bytle.db.jdbc.AnsiDataPath;
import net.bytle.db.jdbc.SqlDataStore;
import net.bytle.db.jdbc.JdbcDataStoreExtension;
import net.bytle.db.jdbc.JdbcDataSystemSql;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.SqlDataType;
import oracle.jdbc.OracleTypes;

import java.sql.Types;

/**
 *
 */
public class OraDataStoreExtension extends JdbcDataStoreExtension {

  public OraDataStoreExtension(SqlDataStore jdbcDataStore) {
    super(jdbcDataStore);
  }

  // Numeric: https://docs.oracle.com/cd/B28359_01/server.111/b28318/datatype.htm#CNCPT313
  @Override
  public void updateSqlDataType(SqlDataType sqlDataType) {

    switch (sqlDataType.getTypeCode()) {
      case OracleTypes.INTERVALDS:
        sqlDataType
          .setTypeName("INTERVALDS")
          .setClazz(oracle.sql.INTERVALDS.class);
        break;
      case OracleTypes.INTERVALYM:
        sqlDataType
          .setTypeName("INTERVAL_YEAR_MONTH")
          .setClazz(oracle.sql.INTERVALYM.class);
        break;
      case OracleTypes.LONGVARBINARY:
        sqlDataType
          .setTypeName("LONG RAW")
          .setClazz(oracle.sql.RAW.class);
        break;
      case Types.LONGVARCHAR:
        sqlDataType
          .setTypeName("LONG");
        break;
      case OracleTypes.NUMBER:
        // https://docs.oracle.com/cd/B28359_01/server.111/b28285/sqlqr06.htm#CHDJJEEA
        sqlDataType
          .setTypeName("NUMBER")
          .setClazz(oracle.sql.NUMBER.class)
          .setMaxPrecision(38)
          .setMaximumScale(127)
          .setMinimumScale(-84);
        break;
      case Types.DOUBLE:
        // https://docs.oracle.com/cd/B28359_01/server.111/b28285/sqlqr06.htm#CHDJJEEA
        sqlDataType
          .setTypeName("NUMBER")
          .setClazz(oracle.sql.NUMBER.class)
          .setMaxPrecision(38)
          .setMaximumScale(127)
          .setMinimumScale(-84);
        break;
      case Types.NVARCHAR:
        sqlDataType
          .setTypeName("NVARCHAR2");
        break;
      case Types.VARBINARY:
        sqlDataType
          .setTypeName("VARBINARY")
          .setClazz(oracle.sql.RAW.class);
        break;
    }
  }

  @Override
  public String getCreateColumnStatement(ColumnDef columnDef) {
    SqlDataType dataType = columnDef.getDataType();
    switch (dataType.getTypeCode()) {
      case Types.INTEGER:
        return "INTEGER";
      case OracleTypes.INTERVALDS:
        return "INTERVAL DAY (" + columnDef.getPrecision() + ") TO SECOND (" + columnDef.getScale() + ")";
      case OracleTypes.INTERVALYM:
        return "INTERVAL YEAR (" + columnDef.getPrecision() + ") TO MONTH";
      case OracleTypes.LONGVARBINARY:
        return "LONG RAW";
      case Types.LONGNVARCHAR:
        return "LONG";
      case OracleTypes.NUMBER:
        // Bug ? If the scale is -127, it's a float
        Integer precision = columnDef.getPrecision();
        Integer scale = columnDef.getScale();
        if (scale == -127 && precision != 0) {
          return "FLOAT(" + precision + ")";
        } else {
          // Default will take over
          if (precision > 38) {
            precision = 38;
          }
          return "NUMBER(" + precision + "," + scale + ")";
        }
      case Types.VARBINARY:
        // Bug in a Oracle driver where precision is null in a resultSet
        if (columnDef.getPrecision() == 0) {
          return "RAW(2000)"; //TODO: of the max of the data type
        } else {
          return null;
        }
      default:
        return null;
    }
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
  public String getTruncateStatement(AnsiDataPath dataPath) {
    return "truncate from " +
      JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath);
  }


}
