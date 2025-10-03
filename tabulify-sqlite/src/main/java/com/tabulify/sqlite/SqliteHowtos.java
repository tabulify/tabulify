package com.tabulify.sqlite;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.connection.ConnectionAttributeEnumBase;
import net.bytle.exception.InternalException;
import net.bytle.fs.Fs;
import net.bytle.type.KeyNormalizer;

import java.nio.file.Path;

import static com.tabulify.jdbc.SqlConnectionAttributeEnum.DRIVER;
import static com.tabulify.sqlite.SqliteProvider.HOWTO_SQLITE_NAME;
import static com.tabulify.sqlite.SqliteProvider.HOWTO_SQLITE_TARGET_NAME;

public class SqliteHowtos {

  private static final String SQLITE_EXTENSION = "sqlite3";

  public static SqliteConnection getConnection(Tabular tabular, KeyNormalizer howtoSqliteName) {

    if (howtoSqliteName.equals(HOWTO_SQLITE_NAME)) {
      Attribute sqliteName = tabular.getVault().createAttribute(ConnectionAttributeEnumBase.NAME, HOWTO_SQLITE_NAME, Origin.DEFAULT);
      String sqliteConnectionString = getSqliteConnectionString(HOWTO_SQLITE_NAME, tabular.getTabliUserHome());
      Attribute sqliteUri = tabular.getVault().createAttribute(ConnectionAttributeEnumBase.URI, sqliteConnectionString, Origin.DEFAULT);
      return (SqliteConnection) new SqliteConnection(tabular, sqliteName, sqliteUri)
        .addAttribute(DRIVER, "org." + HOWTO_SQLITE_NAME + ".JDBC", Origin.DEFAULT)
        .setComment("The " + HOWTO_SQLITE_NAME + " default connection");
    }

    if (howtoSqliteName.equals(HOWTO_SQLITE_TARGET_NAME)) {
      Attribute sqliteTargetName = tabular.getVault().createAttribute(ConnectionAttributeEnumBase.NAME, HOWTO_SQLITE_TARGET_NAME, Origin.DEFAULT);
      String sqliteTargetConnectionString = getSqliteConnectionString(HOWTO_SQLITE_TARGET_NAME, tabular.getTabliUserHome());
      Attribute sqliteTargetUri = tabular.getVault().createAttribute(ConnectionAttributeEnumBase.URI, sqliteTargetConnectionString, Origin.DEFAULT);
      return (SqliteConnection) new SqliteConnection(tabular, sqliteTargetName, sqliteTargetUri)
        .addAttribute(DRIVER, "org." + HOWTO_SQLITE_NAME + ".JDBC", Origin.DEFAULT)
        .setComment("The default " + HOWTO_SQLITE_NAME + " target (Sqlite cannot read and write with the same connection)");
    }

    throw new InternalException("Unknown howto sqlite connections: " + howtoSqliteName);

  }


  /**
   * @param connectionName the name of the connection
   * @param userHomePath   where to store the database (default to tabli user home)
   * @return a JDBC connection string for the default data vault
   */
  public static String getSqliteConnectionString(KeyNormalizer connectionName, Path userHomePath) {

    assert userHomePath != null;
    Path dbFile = userHomePath.resolve(connectionName + "." + SQLITE_EXTENSION);
    Fs.createDirectoryIfNotExists(dbFile.getParent());
    String rootWindows = "///";
    return "jdbc:" + HOWTO_SQLITE_NAME + ":" + rootWindows + dbFile.toString().replace("\\", "/");

  }
}
