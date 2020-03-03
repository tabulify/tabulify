package net.bytle.db.jdbc.Hana;

import net.bytle.db.jdbc.*;
import net.bytle.db.model.*;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * */
public class JdbcDataStoreExtensionIHana extends SqlDataSystem {



  public JdbcDataStoreExtensionIHana(SqlDataStore jdbcDataStore) {
    super(jdbcDataStore);
  }

  public void updateSqlDataType(SqlDataType sqlDataType) {
    switch (sqlDataType.getTypeCode()) {
      case Types.VARCHAR:
        sqlDataType
          .setTypeName("NVARCHAR");
    }
  }

  @Override
  public String createColumnStatement(ColumnDef columnDef) {
    SqlDataType dataType = columnDef.getDataType();
    switch (dataType.getTypeCode()) {
      case Types.VARCHAR:
      // VARCHAR is having length in bytes (not in CHAR !)
      // The VARCHAR(n) data type specifies a variable-length character string, where n :
      //    * indicates the maximum length in bytes
      //    * and is an integer between 1 and 5000.
      //        * https://help.sap.com/saphelp_hanaplatform/helpdata/en/20/a1569875191014b507cf392724b7eb/content.htm
      //
      // Example: The following doesn't fit in a VARCHAR(35)
      //        * select length(TO_VARCHAR('TØJEKSPERTEN HØRSHOLM APS - NR. 252')) from dummy;
        return "NVARCHAR ("+columnDef.getPrecision()+")";
      default:
      return null;
    }
  }



  /**
   * Returns statement to create the table
   *
   * @param sqlDataPath
   * @return
   */
  @Override
  public List<String> createTableStatements(SqlDataPath sqlDataPath) {


    List<String> statements = new ArrayList<>();

    String statement = "create ";

    // String tableType = tableDef.getCreateProperties().getProperty("after_create");
    //        if (tableType != null){
    //            statement += tableType;
    //        }
    statement += " table " + sqlDataPath.getName() + " (\n"
      + createColumnsStatement(sqlDataPath)
      + "\n)";

    statements.add(statement);
    final PrimaryKeyDef primaryKey = sqlDataPath.getOrCreateDataDef().getPrimaryKey();
    if (primaryKey != null) {
      statements.add(createPrimaryKeyStatement(sqlDataPath));
    }

    for (ForeignKeyDef foreignKeyDef : sqlDataPath.getOrCreateDataDef().getForeignKeys()) {
      statements.add(createForeignKeyStatement(foreignKeyDef));
    }

    for (UniqueKeyDef uniqueKeyDef : sqlDataPath.getOrCreateDataDef().getUniqueKeys()) {
      statements.add(createUniqueKeyStatement(uniqueKeyDef));
    }

    return statements;

  }



  @Override
  public String truncateStatement(SqlDataPath dataPath) {
    StringBuilder truncateStatementBuilder = new StringBuilder().append("truncate from ");
    truncateStatementBuilder.append(JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath));
    return truncateStatementBuilder.toString();
  }

}
