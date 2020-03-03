package net.bytle.db.jdbc;

import net.bytle.db.model.ForeignKeyDef;

import java.sql.SQLException;
import java.util.logging.Logger;


public class DbDdl {

  private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());




















  public static void deleteAllRecordsTable(SqlDataPath dataPath) {

    try {
      String dropTableStatement = "delete from " + JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath);
      dataPath.getDataStore().getCurrentConnection().createStatement().execute(dropTableStatement);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public static void dropForeignKey(ForeignKeyDef foreignKeyDef) {
    try {
      final SqlDataPath dataPath = (SqlDataPath) foreignKeyDef.getTableDef().getDataPath();
      String dropTableStatement = "ALTER TABLE " + JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath)
        + " DROP CONSTRAINT " + foreignKeyDef.getName();
      dataPath.getDataStore().getCurrentConnection().createStatement().execute(dropTableStatement);
      LOGGER.info("Foreign Key: " + foreignKeyDef.getName() + " on the table " + dataPath.toString() + " were deleted.");
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

}
