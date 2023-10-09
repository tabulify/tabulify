package net.bytle.db.sqlite;

import net.bytle.db.connection.ConnectionAttValueBooleanDataType;
import net.bytle.db.connection.ConnectionAttValueTimeDataType;
import net.bytle.db.jdbc.SqlConnection;
import net.bytle.db.jdbc.SqlConnectionMetadata;

public class SqliteConnectionMetadata extends SqlConnectionMetadata {

  public SqliteConnectionMetadata(SqlConnection sqlConnection) {

    super(sqlConnection);

    /**
     * Boolean is not supported by Sqlite
     */
    this.setBooleanDataType(ConnectionAttValueBooleanDataType.Binary);
    /**
     * Sql data type not
     */
    if (getDateDataTypeOrDefault() == ConnectionAttValueTimeDataType.NATIVE) {
      this.setDateDataType(ConnectionAttValueTimeDataType.SQL_LITERAL);
    }
    if (getTimestampDataType() == ConnectionAttValueTimeDataType.NATIVE) {
      this.setTimestampDataType(ConnectionAttValueTimeDataType.SQL_LITERAL);
    }
    if (getTimeDataType() == ConnectionAttValueTimeDataType.NATIVE) {
      this.setTimeDataType(ConnectionAttValueTimeDataType.SQL_LITERAL);
    }
  }

  /**
   * Overwrite driver
   * It returns 1 for metadata.isCatalogAtStart
   *
   * @return the max names in a path
   */
  @Override
  public Integer getMaxNamesInPath() {
    return 1;
  }

  @Override
  public Integer getMaxWriterConnection() {
    return 1;
  }

}
