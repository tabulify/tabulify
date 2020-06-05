package net.bytle.db.tpc;

import net.bytle.db.database.DataStore;
import net.bytle.db.spi.DataStoreProvider;

public class TpcDataStoreProvider extends DataStoreProvider {


  public static final String TPCDS_SCHEME = "tpcds";


  @Override
  public DataStore createDataStore(String name, String url) {
    return new TpcDataStore(name, url);
  }

  @Override
  public boolean accept(String url) {
    return url.toLowerCase().startsWith(TPCDS_SCHEME);
  }

}
