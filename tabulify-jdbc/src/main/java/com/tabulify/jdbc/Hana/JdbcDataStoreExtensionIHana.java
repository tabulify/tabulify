package com.tabulify.jdbc.Hana;

import com.tabulify.engine.ForeignKeyDag;
import com.tabulify.jdbc.SqlDataPath;
import com.tabulify.jdbc.SqlConnection;
import com.tabulify.jdbc.SqlDataSystem;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.SqlDataType;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * */
public class JdbcDataStoreExtensionIHana extends SqlDataSystem {



  public JdbcDataStoreExtensionIHana(SqlConnection jdbcDataStore) {
    super(jdbcDataStore);
  }

  public void updateSqlDataType(SqlDataType sqlDataType) {
    switch (sqlDataType.getTypeCode()) {
      case Types.VARCHAR:
        sqlDataType
          .setSqlName("NVARCHAR");
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
  public String createTableStatement(SqlDataPath sqlDataPath) {

    // String tableType = tableDef.getCreateProperties().getProperty("after_create");
    //        if (tableType != null){
    //            statement += tableType;
    //        }
    return super.createTableStatement(sqlDataPath);

  }


  /**
   * https://help.sap.com/viewer/4fe29514fd584807ac9f2a04f6754767/2.0.03/en-US/20fe29f0751910149904f0c5c3201cfa.html
   * @param sqlDataPaths
   * @return
   */
  @Override
  public List<String> createTruncateStatement(List<SqlDataPath> sqlDataPaths) {
    List<SqlDataPath> dropOrderDataPaths = ForeignKeyDag.createFromPaths(sqlDataPaths).getDropOrdered();
    List<String> truncateStatements  = new ArrayList<>();
    for (SqlDataPath dropOrderDataPath : dropOrderDataPaths) {
      String stringBuilder = "truncate from " +
        dropOrderDataPath.toSqlStringPath();
      truncateStatements.add(stringBuilder);
    }
    return truncateStatements;
  }

}
