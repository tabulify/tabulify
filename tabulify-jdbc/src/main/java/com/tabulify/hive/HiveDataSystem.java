package com.tabulify.hive;

import com.tabulify.engine.ForeignKeyDag;
import com.tabulify.jdbc.SqlDataPath;
import com.tabulify.jdbc.SqlConnection;
import com.tabulify.jdbc.SqlDataSystem;
import com.tabulify.model.ColumnDef;

import java.util.ArrayList;
import java.util.List;

public class HiveDataSystem extends SqlDataSystem {
  public HiveDataSystem(SqlConnection sqlConnection) {
    super(sqlConnection);
  }

  @Override
  protected String createColumnStatement(ColumnDef columnDef) {
    // Hive does not support the not null statement
    return createQuotedName(columnDef.getColumnName()) + " " + createDataTypeStatement(columnDef);
  }

  @Override
  protected List<String> createTruncateStatement(List<SqlDataPath> sqlDataPaths) {
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
