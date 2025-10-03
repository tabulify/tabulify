package com.tabulify.sqlite;

import com.tabulify.jdbc.SqlDataPathRelationDef;
import com.tabulify.jdbc.SqlLog;
import com.tabulify.jdbc.SqlMetaForeignKey;
import com.tabulify.model.SqlDataType;
import com.tabulify.model.SqlDataTypeNullable;
import com.tabulify.spi.Tabulars;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.Maps;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class SqliteDataPathRelationDef extends SqlDataPathRelationDef {


  public SqliteDataPathRelationDef(SqliteDataPath dataPath, Boolean buildFromMeta) {
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
   */
  @Override
  protected void addColumnsFromMetadata() {

    /**
     * Why Overwrite
     * Because the driver returns 20000000 and no data type name
     */

    // Note that pragma does not support prepared statement
    // `PRAGMA table_info(?)` would yield: [SQLITE_ERROR] SQL error or missing database (near "?": syntax error)
    String sql = "PRAGMA table_info('" + getDataPath().getName() + "')";
    try (
      Statement statement = this.getDataPath().getConnection().getCurrentJdbcConnection().createStatement();
      ResultSet resultSet = statement.executeQuery(sql)
    ) {
      while (resultSet.next()) {

        String columnName = resultSet.getString("name");
        // ie INTEGER(50)
        String dataType = resultSet.getString("type");
        SqliteTypeParser type = SqliteTypeParser.create(dataType);
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
        // Not sure what to do with that
        SqlDataType<?> jdbcDataType = getDataPath().getConnection().getSqlDataType(KeyNormalizer.createSafe(typeCodeName));
        if (jdbcDataType == null) {
          throw new RuntimeException("Unable to find a type for the name (" + typeCodeName + ") for the column (" + columnName + ") of the table (" + this.getDataPath() + ")");
        }
        int notnull = resultSet.getInt("notnull");
        SqlDataTypeNullable notNull;
        if (notnull == 0) {
          notNull = SqlDataTypeNullable.NULLABLE;
        } else {
          notNull = SqlDataTypeNullable.NO_NULL;
        }
        this.getOrCreateColumn(columnName, jdbcDataType)
          .setPrecision(type.getPrecision())
          .setScale(type.getScale())
          .setIsAutoincrement(null)
          .setIsGeneratedColumn(null)
          .setNullable(notNull);
      }
    } catch (SQLException e) {
      // we don't pass a bigger message to give any context
      // because the data path may be created temporarily
      // The user would see:
      // Error trying to retrieve the meta from ("tmp_tabulify_4fed3943bb14f96aeff44b6d13c4f253"@sqlite)
      // We let the caller gives the good context
      throw new IllegalStateException(e.getMessage(), e);
    }
  }


  /**
   * Retrieve the data from foreign_key_list
   * <a href="https://www.sqlite.org/pragma.html#pragma_foreign_key_list">...</a>
   */
  @Override
  protected void addForeignKeysFromMetadata() {

    Connection connection = this.getDataPath().getConnection().getCurrentJdbcConnection();
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
        throw new RuntimeException(s, e);
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

    Connection connection = this.getDataPath().getConnection().getCurrentJdbcConnection();
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
          String name = resultSet.getString("name");
          mapPkColumns.put(pkIndex, name);
        }
      }
    } catch (SQLException e) {
      // we don't pass a bigger message to give any context
      // because the data path may be created temporarily
      // The user would see:
      // Error trying to retrieve the meta from ("tmp_tabulify_4fed3943bb14f96aeff44b6d13c4f253"@sqlite)
      // We let the caller give the good context
      throw new IllegalStateException(e.getMessage(), e);
    }
    if (!mapPkColumns.isEmpty()) {
      ArrayList<String> sortedColumnsList = new ArrayList<>(Maps.getMapSortByKey(mapPkColumns).values());
      this.setPrimaryKey(sortedColumnsList);
    }

  }
}
