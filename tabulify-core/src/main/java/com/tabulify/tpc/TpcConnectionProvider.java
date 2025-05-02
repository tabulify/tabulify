package com.tabulify.tpc;

import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.spi.ConnectionProvider;
import com.tabulify.conf.Attribute;

public class TpcConnectionProvider extends ConnectionProvider {


  public static final String TPCDS_SCHEME = "tpcds";


  @Override
  public Connection createConnection(Tabular tabular, Attribute name, Attribute uri) {
    return new TpcConnection(tabular, name, uri);
  }

  @Override
  public boolean accept(Attribute uri) {
    return uri.getValueOrDefaultAsStringNotNull().toLowerCase().startsWith(TPCDS_SCHEME);
  }

}
