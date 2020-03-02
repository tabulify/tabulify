package net.bytle.db.sqlite;

import net.bytle.db.jdbc.AnsiDataDef;
import net.bytle.db.model.SqlDataType;
import net.bytle.db.spi.Tabulars;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class SqliteDataDef extends AnsiDataDef {

  public SqliteDataDef(SqliteDataPath dataPath, Boolean buildFromMeta) {
    super(dataPath, buildFromMeta);
  }

  @Override
  public SqliteDataPath getDataPath() {
    return (SqliteDataPath) super.dataPath;
  }

  /**
   * Add columns to a table
   * This function was created because Sqlite does not really implements a JDBC type
   * Sqlite gives them back via a string
   *
   * @return true if the columns were added to the table
   */
  @Override
  protected void addColumnsFromMetadata() {

    // Because the driver returns 20000000 and no data type name
    try (
      Statement statement = this.getDataPath().getDataStore().getCurrentConnection().createStatement();
      ResultSet resultSet = statement.executeQuery("PRAGMA table_info('" + dataPath.getName() + "')");
    ) {
      while (resultSet.next()) {

        String columnName = resultSet.getString("name");
        // ie INTEGER(50)
        String dataType = resultSet.getString("type");
        SqliteType type = SqliteType.get(this.getDataPath().getDataStore(), dataType);
        String typeCodeName = type.getTypeName();
        // SQlite use class old data type
        if (typeCodeName.equals("TEXT")) {
          typeCodeName = "VARCHAR";
        }
        // Not sure what to do with that
        // Integer typeCode = type.getTypeCode();
        SqlDataType jdbcDataType = dataPath.getDataStore().getSqlDataType(typeCodeName);
        if (jdbcDataType == null) {
          throw new RuntimeException("Unable to find a type for the name " + typeCodeName);
        }

        this.getOrCreateColumn(columnName, jdbcDataType.getClazz())
          .typeCode(jdbcDataType.getTypeCode())
          .precision(type.getPrecision())
          .scale(type.getScale())
          .isAutoincrement("")
          .isGeneratedColumn("")
          .setNullable(resultSet.getInt("notnull") == 0 ? 1 : 0);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void addForeignKeysFromMetadata() {

    Connection connection = this.getDataPath().getDataStore().getCurrentConnection();
    Map<Integer, List<String>> foreignKeys = new HashMap<>();
    final String sql = "PRAGMA foreign_key_list('" + dataPath.getName() + "')";
    try (
      Statement statement = connection.createStatement();
      ResultSet resultSet = statement.executeQuery(sql);
    ) {
      while (resultSet.next()) {
        String parentTable = resultSet.getString("table");
        String fromColumn = resultSet.getString("from");
        Integer id = resultSet.getInt("id");
        foreignKeys.put(id, Arrays.asList(parentTable, fromColumn));
      }
    } catch (SQLException e) {
      if (!e.getMessage().equals("query does not return ResultSet")) {
        LOGGER.error("An error was seen while running this SQL statement: " + sql);
        throw new RuntimeException(e);
      }

    }

    // Sqlite seems to preserve the order of the foreign keys but descendant
    // Hack to get it right
    for (int i = foreignKeys.size() - 1; i >= 0; i--) {
      final List<String> foreignKey = foreignKeys.get(i);
      final String foreignTableName = foreignKey.get(0);
      final String nativeTableColumn = foreignKey.get(1);
      SqliteDataPath foreignDataPath = (SqliteDataPath) this.getDataPath().getDataStore().getDefaultDataPath(foreignTableName);

      // This is possible in Sqlite to have foreign key on table that does not exist
      if (Tabulars.exists(foreignDataPath)) {
        this.addForeignKey(
          foreignDataPath,
          nativeTableColumn
        );
      }

    }

  }

  /**
   * Due to a bug in JDBC
   * (ie the primary column names had the foreign key - it seems that they parse the create statement)
   * We created this hack
   *
   */
  @Override
  protected void addPrimaryKeyFromMetaData() {

    Connection connection = this.getDataPath().getDataStore().getCurrentConnection();
    List<String> columns = new ArrayList<>();
    final String sql = "PRAGMA table_info('" + dataPath.getName() + "')";
    try (
      Statement statement = connection.createStatement();
      ResultSet resultSet = statement.executeQuery(sql)
    ) {
      while (resultSet.next()) {
        int pk = resultSet.getInt("pk");
        if (pk == 1) {
          columns.add(resultSet.getString("name"));
        }
      }
    } catch (SQLException e) {
      LOGGER.error("Sql problem with the following sql (" + sql + ")");
      throw new RuntimeException(e);
    }
    if (columns.size() > 0) {
      this.setPrimaryKey(columns);
    }

  }
}
