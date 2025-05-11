package com.tabulify.sqlite;

import com.tabulify.jdbc.SqlLog;
import com.tabulify.jdbc.SqlMediaType;
import com.tabulify.jdbc.SqlMetaForeignKey;
import com.tabulify.jdbc.SqlRelationDef;
import com.tabulify.model.SqlDataType;
import com.tabulify.spi.Tabulars;
import net.bytle.type.Maps;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class SqliteRelationDef extends SqlRelationDef {

  public SqliteRelationDef(SqliteDataPath dataPath, Boolean buildFromMeta) {
    super(dataPath, buildFromMeta);
  }

  @Override
  public SqliteDataPath getDataPath() {
    return (SqliteDataPath) super.getDataPath();
  }

  /**
   * Add columns to a table
   * This function was created because Sqlite does not really implement a JDBC type
   * Sqlite gives them back via a string
   *
   */
  @Override
  protected void addColumnsFromMetadata() {

    // Because the driver returns 20000000 and no data type name
    String sql = "PRAGMA table_info('" + getDataPath().getName() + "')";
    try (
      Statement statement = this.getDataPath().getConnection().getCurrentConnection().createStatement();
      ResultSet resultSet = statement.executeQuery(sql)
    ) {
      while (resultSet.next()) {

        String columnName = resultSet.getString("name");
        // ie INTEGER(50)
        String dataType = resultSet.getString("type");
        SqliteType type = SqliteType.create(this.getDataPath().getConnection(), dataType);
        String typeCodeName = type.getTypeName();
        if (typeCodeName == null) {

          /**
           * Null
           * example:
           *   * create table foo(bar) will get you a null type code on bar
           *   * A view expression has also no data type
           */
          SqlLog.LOGGER_DB_JDBC.info("For the column name (" + columnName + "), the text data type was used because it's unknown in the table (" + getDataPath() + ")");
          typeCodeName = "TEXT";

        }
        // SQLite use class old data type
        switch (typeCodeName) {
          case "TEXT":
            typeCodeName = "VARCHAR";
            break;
          case "NUM":
            /**
             * A `create as table` statement create a NUM
             */
            typeCodeName = "real";
            break;
          case "INT":
            /**
             * A `create as table` statement create an INT
             */
            typeCodeName = "integer";
            break;
        }
        // Not sure what to do with that
        // Integer typeCode = type.getTypeCode();
        SqlDataType jdbcDataType = getDataPath().getConnection().getSqlDataType(typeCodeName);
        if (jdbcDataType == null) {
          throw new RuntimeException("Unable to find a type for the name (" + typeCodeName + ") for the column (" + columnName + ") of the table (" + this.getDataPath() + ")");
        }
        int notnull = resultSet.getInt("notnull");

        this.getOrCreateColumn(columnName, jdbcDataType, jdbcDataType.getSqlClass())
          .precision(type.getPrecision())
          .scale(type.getScale())
          .setIsAutoincrement(null)
          .setIsGeneratedColumn("")
          .setNullable(notnull == 0 ? 1 : 0);
      }
    } catch (SQLException e) {
      processPragmaTableInfoException(e);
    }
  }

  private void processPragmaTableInfoException(SQLException e) {
    if (this.getDataPath().getMediaType() == SqlMediaType.VIEW) {
      /**
       * Sqlite is so versatile that you can store a bad view
       */
      String message = "Error trying to retrieve the view meta from (" + this.getDataPath() + "): " + e.getMessage();
      Sqlites.LOGGER_SQLITE.warning(message);
    } else {
      String message = "Error trying to retrieve the table meta from (" + this.getDataPath() + "): " + e.getMessage();
      throw new IllegalStateException(message, e);
    }
  }

  /**
   * Retrieve the data from foreign_key_list
   * https://www.sqlite.org/pragma.html#pragma_foreign_key_list
   */
  @Override
  protected void addForeignKeysFromMetadata() {

    Connection connection = this.getDataPath().getConnection().getCurrentConnection();
    Map<String, SqlMetaForeignKey> foreignKeys = new HashMap<>();
    final String sql = "PRAGMA foreign_key_list('" + getDataPath().getName() + "')";
    try (
      Statement statement = connection.createStatement();
      ResultSet resultSet = statement.executeQuery(sql)
    ) {
      while (resultSet.next()) {

        /**
         * The parent table (the table with the primary key)
         */
        String parentTable = resultSet.getString("table");
        /**
         * id is the id of the foreign key
         */
        String id = resultSet.getString("id");

        /**
         * Create if not present
         */
        SqlMetaForeignKey sqliteMetaForeignKey = foreignKeys.computeIfAbsent(id, s -> new SqlMetaForeignKey(null, null, parentTable, null, null, this.getDataPath().getName(), id));

        /**
         * Get the column mapping
         */
        String pkColumn = resultSet.getString("to");
        String fkColumn = resultSet.getString("from");
        short seq = resultSet.getShort("seq");
        sqliteMetaForeignKey.addColumnMapping(seq, pkColumn, fkColumn);

      }
    } catch (SQLException e) {
      if (!e.getMessage().equals("query does not return ResultSet")) {
        String s = "An error was seen while running this SQL statement: " + sql;
        throw new RuntimeException(s,e);
      }

    }

    foreignKeys.values().forEach(v -> {

      // This is possible in Sqlite to have foreign key on table that does not exist
      SqliteDataPath primaryDataPath = (SqliteDataPath) this.getDataPath().getConnection().getDataPath(v.getPrimaryTableName());
      if (Tabulars.exists(primaryDataPath)) {
        this.addForeignKey(
          primaryDataPath.getOrCreateRelationDef().getPrimaryKey(),
          v.getForeignKeyColumns()
        );

      }
    });
  }


  /**
   * Due to a bug in JDBC
   * (ie the primary column names had the foreign key - it seems that they parse the `create` statement)
   * We created this hack
   */
  @Override
  protected void addPrimaryKeyFromMetaData() {

    Connection connection = this.getDataPath().getConnection().getCurrentConnection();
    Map<Integer, String> mapPkColumns = new TreeMap<>();
    final String sql = "PRAGMA table_info('" + getDataPath().getName() + "')";
    try (
      Statement statement = connection.createStatement();
      ResultSet resultSet = statement.executeQuery(sql)
    ) {
      while (resultSet.next()) {
        /**
         * The primary key index
         * 1 for the first one, 2 for the second
         */
        int pkIndex = resultSet.getInt("pk");
        if (pkIndex > 0) {
          mapPkColumns.put(pkIndex, resultSet.getString("name"));
        }
      }
    } catch (SQLException e) {
      processPragmaTableInfoException(e);
    }
    if (mapPkColumns.size() > 0) {
      ArrayList<String> sortedColumnsList = new ArrayList<>(Maps.getMapSortByKey(mapPkColumns).values());
      this.setPrimaryKey(sortedColumnsList);
    }

  }
}
