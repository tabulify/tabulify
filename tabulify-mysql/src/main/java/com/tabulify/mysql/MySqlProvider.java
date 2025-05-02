package com.tabulify.mysql;

import com.tabulify.Tabular;
import com.tabulify.jdbc.SqlDataStoreProvider;
import com.tabulify.conf.Attribute;

public class MySqlProvider extends SqlDataStoreProvider {

  public final String URL_PREFIX = "jdbc:mysql:";

  @Override
  public MySqlConnection getJdbcDataStore(Tabular tabular, Attribute name, Attribute url) {

    return new MySqlConnection(tabular, name, url);

  }

  @Override
  public boolean accept(Attribute url) {
    return url.getValueOrDefaultAsStringNotNull().startsWith(URL_PREFIX);
  }

}
