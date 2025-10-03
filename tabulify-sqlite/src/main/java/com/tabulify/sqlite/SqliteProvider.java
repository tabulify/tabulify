package com.tabulify.sqlite;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.jdbc.SqlConnection;
import com.tabulify.jdbc.SqlConnectionProvider;
import net.bytle.type.KeyNormalizer;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SqliteProvider extends SqlConnectionProvider {

  public static final KeyNormalizer HOWTO_SQLITE_NAME = KeyNormalizer.createSafe("sqlite");
  public static final KeyNormalizer HOWTO_SQLITE_TARGET_NAME = KeyNormalizer.createSafe("sqlite_target");
  public static final String URL_PREFIX = "jdbc:" + HOWTO_SQLITE_NAME + ":";
  public static final String ROOT = "///";

  @Override
  public SqliteConnection createSqlConnection(Tabular tabular, Attribute name, Attribute url) {

    return new SqliteConnection(tabular, name, url);

  }

  @Override
  public boolean accept(Attribute url) {
    return url.getValueOrDefaultAsStringNotNull().startsWith(URL_PREFIX);
  }


  @Override
  public Set<SqlConnection> getHowToConnections(Tabular tabular) {

    return Stream.of(HOWTO_SQLITE_NAME, HOWTO_SQLITE_TARGET_NAME)
      .map(n -> SqliteHowtos.getConnection(tabular, n))
      .collect(Collectors.toSet());

  }


}
