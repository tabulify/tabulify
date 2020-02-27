package net.bytle.db.jdbc.Hive;

import net.bytle.db.jdbc.AnsiDataPath;
import net.bytle.db.jdbc.SqlDataStore;
import net.bytle.db.jdbc.JdbcDataStoreExtension;
import net.bytle.db.jdbc.JdbcDataSystemSql;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 *
 */
public class JdbcDataStoreExtensionIHive extends JdbcDataStoreExtension {


  public JdbcDataStoreExtensionIHive(SqlDataStore jdbcDataStore) {
    super(jdbcDataStore);
  }

  @Override
  public void updateSqlDataType(SqlDataType sqlDataType) {
    switch (sqlDataType.getTypeCode()) {
      case Types.NUMERIC:
        sqlDataType
          .setTypeName("DECIMAL");
      case Types.TIME:
        // Time doesn't exist, we try to make it a timestamp
        sqlDataType
          .setTypeName("TIMESTAMP");
    }
  }

  @Override
  public String getCreateColumnStatement(ColumnDef columnDef) {
    SqlDataType dataType = columnDef.getDataType();
    switch (dataType.getTypeCode()) {
      case Types.CHAR:
        return "CHAR("+columnDef.getPrecision()+")";
      case Types.INTEGER:
        // No precision
        // https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Types#LanguageManualTypes-IntegralTypes(TINYINT,SMALLINT,INT/INTEGER,BIGINT)
        return "INT";
      case Types.NUMERIC:
        return "DECIMAL("+columnDef.getPrecision()+","+columnDef.getScale()+")";
      case Types.SMALLINT:
        // No precision
        // https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Types#LanguageManualTypes-IntegralTypes(TINYINT,SMALLINT,INT/INTEGER,BIGINT)
        return "SMALLINT";
      case Types.TIME:
        // Time doesn't exist, we try to make it a timestamp
        return "TIMESTAMP";
      case Types.VARCHAR:
        return "VARCHAR("+columnDef.getPrecision()+")";
      default:
        return null;
    }
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
  public String getTruncateStatement(AnsiDataPath dataPath) {
    StringBuilder truncateStatementBuilder = new StringBuilder().append("truncate from ");
    truncateStatementBuilder.append(JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath));
    return truncateStatementBuilder.toString();
  }

}
