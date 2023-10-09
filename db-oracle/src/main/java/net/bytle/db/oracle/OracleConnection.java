package net.bytle.db.oracle;

import net.bytle.db.Tabular;
import net.bytle.db.jdbc.SqlConnection;
import net.bytle.db.jdbc.SqlDataPath;
import net.bytle.db.jdbc.SqlDataSystem;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.SqlDataType;
import net.bytle.type.Variable;
import oracle.jdbc.OracleTypes;

import java.sql.Types;

/**
 *
 */
public class OracleConnection extends SqlConnection {

  public OracleConnection(Tabular tabular, Variable name, Variable url) {
    super(tabular, name, url);
  }

  @Override
  public SqlDataSystem getDataSystem() {
    return new OracleSystem(this);
  }




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
  public OracleConnectionMetadata getMetadata() {
    return new OracleConnectionMetadata(this);
  }

  public String getTruncateStatement(SqlDataPath dataPath) {
    return "truncate from " + dataPath.toSqlStringPath();
  }


}
