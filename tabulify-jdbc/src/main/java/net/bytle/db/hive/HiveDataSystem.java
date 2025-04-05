package net.bytle.db.hive;

import net.bytle.db.engine.ForeignKeyDag;
import net.bytle.db.jdbc.SqlDataPath;
import net.bytle.db.jdbc.SqlConnection;
import net.bytle.db.jdbc.SqlDataSystem;
import net.bytle.db.model.ColumnDef;

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
