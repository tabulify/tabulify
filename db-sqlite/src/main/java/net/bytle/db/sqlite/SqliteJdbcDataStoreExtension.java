package net.bytle.db.sqlite;

import net.bytle.db.jdbc.*;
import net.bytle.db.model.*;
import net.bytle.db.spi.Tabulars;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class SqliteJdbcDataStoreExtension extends JdbcDataStoreExtension {

  private static final Logger LOGGER = LoggerFactory.getLogger(SqliteJdbcDataStoreExtension.class);

  public SqliteJdbcDataStoreExtension(JdbcDataStore jdbcDataStore) {
    super(jdbcDataStore);
  }

  @Override
  public void updateSqlDataType(SqlDataType sqlDataType) {
    // Don't change the name of the data type
    // because of the versality of sqlite, a user may create a column with a TEXT or VARCHAR
    switch (sqlDataType.getTypeCode()) {
      case Types.VARCHAR:
        sqlDataType
          .setMaxPrecision(SqliteType.MAX_LENGTH);
        break;
    }
  }

  @Override
  public String getCreateColumnStatement(ColumnDef columnDef) {
    switch (columnDef.getDataType().getTypeCode()) {
      default:
        return null;
    }

  }


  @Override
  public String getNormativeSchemaObjectName(String objectName) {
    return "\"" + objectName + "\"";
  }

  /**
   * Related ?
   * {@link DatabaseMetaData#getMaxConnections()}
   * Sqlite can't have several connection
   *
   * @return
   */
  @Override
  public Integer getMaxWriterConnection() {
    return 1;
  }

  /**
   * Returns statement to create the table
   * SQLlite has limited support on the alter statement
   * The primary key shoud be in the create statement.
   * See https://sqlite.org/faq.html#q11
   *
   * @param dataPath
   * @return a create statement https://www.sqlite.org/lang_createtable.html
   */
  @Override
  public List<String> getCreateTableStatements(JdbcDataPath dataPath) {

    List<String> statements = new ArrayList<>();
    StringBuilder statement = new StringBuilder();
    statement.append("CREATE TABLE " + JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath) + " (\n");
    TableDef tableDef = dataPath.getDataDef();
    if (tableDef == null) {
      throw new RuntimeException("The dataPath (" + dataPath.toString() + ") has no columns definitions. We can't create a table from then");
    }
    for (int i = 0; i < tableDef.getColumnDefs().size(); i++) {
      ColumnDef columnDef = tableDef.getColumnDefs().get(i);
      statement.append(DbDdl.getColumnStatementForCreateTable(columnDef));
      if (i != tableDef.getColumnDefs().size() - 1) {
        statement.append(",\n");
      }
    }

    // Pk
    final PrimaryKeyDef primaryKey = tableDef.getPrimaryKey();
    if (tableDef.getPrimaryKey() != null) {
      statement.append(",\nPRIMARY KEY (");
      for (int i = 0; i < primaryKey.getColumns().size(); i++) {
        ColumnDef columnDef = primaryKey.getColumns().get(i);
        statement.append(columnDef.getColumnName());
        if (i < primaryKey.getColumns().size() - 1) {
          statement.append(", ");
        }
      }
      statement.append(")");
    }

    // Fk
    // http://www.hwaci.com/sw/sqlite/foreignkeys.html
    // The parent table is the table that a foreign key constraint refers to.
    // The child table is the table that a foreign key constraint is applied to and the table that contains the REFERENCES clause.
    //        CREATE TABLE song(
    //                songid     INTEGER,
    //                songartist TEXT,
    //                songalbum TEXT,
    //                songname   TEXT,
    //                FOREIGN KEY(songartist, songalbum) REFERENCES album(albumartist, albumname)
    //        );
    final List<ForeignKeyDef> foreignKeyDefs = tableDef.getForeignKeys();
    for (ForeignKeyDef foreignKeyDef : foreignKeyDefs) {

      statement.append(",\nFOREIGN KEY (");

      // Child columns
      List<String> childColumns = foreignKeyDef.getChildColumns().stream()
        .map(s -> s.getColumnName())
        .collect(Collectors.toList());
      statement.append(String.join(",", childColumns));

      statement.append(") REFERENCES ");
      statement.append(foreignKeyDef.getForeignPrimaryKey().getDataDef().getDataPath().getName());
      statement.append("(");

      // Foreign / Parent  columns
      List<String> parentColumns = foreignKeyDef.getForeignPrimaryKey().getColumns()
        .stream()
        .map(s -> s.getColumnName())
        .collect(Collectors.toList());
      statement.append(String.join(",", parentColumns));
      statement.append(")");

    }


    // End statement
    statement.append("\n)");
    statements.add(statement.toString());

    return statements;

  }

  /**
   * Due to a bug in JDBC
   * (ie the primary column names had the foreign key - it seems that they parse the create statement)
   * We created this hack
   *
   * @param tableDef
   */
  public Boolean addPrimaryKey(TableDef tableDef) {

    final JdbcDataPath dataPath = (JdbcDataPath) tableDef.getDataPath();
    Connection connection = dataPath.getDataStore().getCurrentConnection();
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
      tableDef.setPrimaryKey(columns);
    }

    return true;

  }

  public Boolean addForeignKey(TableDef tableDef) {

    final JdbcDataPath dataPath = (JdbcDataPath) tableDef.getDataPath();
    Connection connection = dataPath.getDataStore().getCurrentConnection();
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
      JdbcDataPath foreignDataPath = dataPath.getDataStore().getDataPath(foreignTableName);

      // This is possible in Sqlite to have foreign key on table that does not exist
      if (Tabulars.exists(foreignDataPath)) {
        tableDef.addForeignKey(
          foreignDataPath,
          nativeTableColumn
        );
      }

    }


    return true;

  }

  /**
   * Add columns to a table
   * This function was created because Sqlite does not really implements a JDBC type
   * Sqlite gives them back via a string
   *
   * @param tableDef
   * @return true if the columns were added to the table
   */
  @Override
  public boolean addColumns(TableDef tableDef) {

    // Because the driver returns 20000000 and no data type name
    final JdbcDataPath dataPath = (JdbcDataPath) tableDef.getDataPath();
    try (
      Statement statement = dataPath.getDataStore().getCurrentConnection().createStatement();
      ResultSet resultSet = statement.executeQuery("PRAGMA table_info('" + dataPath.getName() + "')");
    ) {
      while (resultSet.next()) {
        // ie INTEGER(50)
        String dataType = resultSet.getString("type");
        SqliteType type = SqliteType.get(tableDef.getDataPath().getDataStore(), dataType);
        String typeCodeName = type.getTypeName();
        // SQlite use class old data type
        if (typeCodeName.equals("TEXT")) {
          typeCodeName = "VARCHAR";
        }
        // Not sure what to do with that
        // Integer typeCode = type.getTypeCode();
        SqlDataType jdbcDataType = dataPath.getDataStore().getSqlDataType(typeCodeName);
        if (jdbcDataType==null){
          throw new RuntimeException("Unable to find a type for the name "+typeCodeName);
        }
        tableDef.getColumnOf(resultSet.getString("name"), jdbcDataType.getClazz())
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
    return true;
  }

  @Override
  public String getTruncateStatement(JdbcDataPath dataPath) {
    StringBuilder truncateStatementBuilder = new StringBuilder().append("delete from ");
    truncateStatementBuilder.append(JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath));
    return truncateStatementBuilder.toString();
  }

  /**
   * @param path whatever/youwant/db.db
   * @return an JDBC Url from a path
   */
  static public String getJdbcUrl(Path path) {

    Path dirDbFile = path.getParent();
    if (!Files.exists(dirDbFile)) {
      try {
        Files.createDirectory(dirDbFile);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }


    // TODO: what if linux
    String rootWindows = "///";
    return "jdbc:sqlite:" + rootWindows + path.toString().replace("\\", "/");

  }

}
