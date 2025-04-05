package net.bytle.db.tpc;

import net.bytle.db.Tabular;
import net.bytle.db.connection.Connection;
import net.bytle.db.spi.ConnectionProvider;
import net.bytle.type.Variable;

public class TpcConnectionProvider extends ConnectionProvider {


  public static final String TPCDS_SCHEME = "tpcds";


  @Override
  public Connection createConnection(Tabular tabular, Variable name, Variable uri) {
    return new TpcConnection(tabular, name, uri);
  }

  @Override
  public boolean accept(Variable uri) {
    return uri.getValueOrDefaultAsStringNotNull().toLowerCase().startsWith(TPCDS_SCHEME);
  }

}
