package net.bytle.db.database;

import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.TableSystem;

public class DefaultDataStore extends DataStore {

  public DefaultDataStore(String name, String url) {
    super(name, url);
  }

  @Override
  public TableSystem getDataSystem() {
    throw new RuntimeException("No provider was found for datastore ("+getName()+") and the the url ("+getConnectionString()+")");
  }

  @Override
  public DataPath getDataPath(String... parts) {
    throw new RuntimeException("No provider was found for datastore ("+getName()+") and the the url ("+getConnectionString()+")");
  }

  @Override
  public DataPath getCurrentDataPath() {
    throw new RuntimeException("No provider was found for datastore ("+getName()+") and the the url ("+getConnectionString()+")");
  }

}
