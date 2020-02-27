package net.bytle.db.sqlite;

import net.bytle.db.jdbc.AnsiDataPath;
import net.bytle.db.jdbc.AnsiDataStore;
import net.bytle.db.model.SqlDataType;
import net.bytle.type.Strings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Types;
import java.util.Set;
import java.util.stream.Collectors;

public class SqliteDataStore extends AnsiDataStore {


  private SqliteSqlSystem sqliteDataSystem;

  public SqliteDataStore(String name, String url) {
    super(name, url);

  }


  /**
   * @param path whatever/youwant/db.db
   * @return an JDBC Url from a path
   */
  public static SqliteDataStore of(Path path) {
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
    String url = "jdbc:sqlite:" + rootWindows + path.toString().replace("\\", "/");
    return new SqliteDataStore(path.getFileName().toString(), url);
  }

  @Override
  public SqliteDataPath getDataPath(String... names) {

    switch (names.length) {
      case 0:
        throw new RuntimeException("We can't create a path without names");
      case 1:
        switch (names[0]) {
          case AnsiDataPath.CURRENT_WORKING_DIRECTORY:
            return new SqliteDataPath(this, null, AnsiDataPath.CURRENT_WORKING_DIRECTORY, null);
          case AnsiDataPath.PARENT_DIRECTORY:
            throw new RuntimeException("Sqlite does not have the notion of catalog or schema, you can't ask therefore for a parent");
          default:
            return new SqliteDataPath(this, null, null, names[0]);
        }
      case 2:
        if (names[0].equals(AnsiDataPath.CURRENT_WORKING_DIRECTORY)) {
          return new SqliteDataPath(this, null, null, names[1]);
        }
      default:
        throw new RuntimeException(
          Strings.multiline("Sqlite does not support any catalog or schema",
            "A path can not have more than one name. ",
            "We got more than one (" + names + ")"));
    }


  }

  @Override
  public Integer getMaxWriterConnection() {
    return 1;
  }

  @Override
  public SqliteSqlSystem getDataSystem() {
    if (sqliteDataSystem == null) {
      sqliteDataSystem = new SqliteSqlSystem(this);
    }
    return sqliteDataSystem;
  }


  @Override
  public SqlDataType getSqlDataType(String typeName) {
    SqlDataType sqlDataType = super.getSqlDataType(typeName);
    sqlDataType = ModifyDataTypeIfNeeded(sqlDataType);
    return sqlDataType;
  }

  @Override
  public SqlDataType getSqlDataType(Class<?> clazz) {
    SqlDataType sqlDataType = super.getSqlDataType(clazz);
    sqlDataType = ModifyDataTypeIfNeeded(sqlDataType);
    return sqlDataType;
  }

  @Override
  public SqlDataType getSqlDataType(Integer typeCode) {
    SqlDataType sqlDataType = super.getSqlDataType(typeCode);
    sqlDataType = ModifyDataTypeIfNeeded(sqlDataType);
    return sqlDataType;
  }

  @Override
  public Set<SqlDataType> getSqlDataTypes() {
    return super.getSqlDataTypes()
      .stream()
      .map(this::ModifyDataTypeIfNeeded)
      .collect(Collectors.toSet());
  }

  private SqlDataType ModifyDataTypeIfNeeded(SqlDataType sqlDataType) {
    // Don't change the name of the data type
    // because of the versality of sqlite, a user may create a column with a TEXT or VARCHAR
    switch (sqlDataType.getTypeCode()) {
      case Types.VARCHAR:
        sqlDataType
          .setMaxPrecision(SqliteType.MAX_LENGTH);
        break;
    }
    return sqlDataType;
  }
}
