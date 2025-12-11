package com.tabulify.tpc;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.connection.Connection;
import com.tabulify.spi.ConnectionProvider;
import com.tabulify.type.KeyNormalizer;

public class TpcConnectionProvider extends ConnectionProvider {


  public static final KeyNormalizer TPCDS_SCHEME = KeyNormalizer.createSafe("tpcds");


  @Override
  public Connection createConnection(Tabular tabular, Attribute name, Attribute uri) {
    return new TpcConnection(tabular, name, uri);
  }

  @Override
  public boolean accept(Attribute uri) {
    return uri.getValueOrDefaultAsStringNotNull().toLowerCase().startsWith(TPCDS_SCHEME.toString());
  }


}
