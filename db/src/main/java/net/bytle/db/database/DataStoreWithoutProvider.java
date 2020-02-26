package net.bytle.db.database;

import net.bytle.db.spi.DataPathAbs;
import net.bytle.db.spi.ProcessingEngine;
import net.bytle.db.spi.DataSystem;

public class DataStoreWithoutProvider extends DataStore {

  public DataStoreWithoutProvider(String name, String url) {
    super(name, url);
  }

  @Override
  public DataSystem getDataSystem() {
    throw new RuntimeException("No provider was found for datastore ("+getName()+") and the the url ("+getConnectionString()+")");
  }

  @Override
  public DataPathAbs getDataPath(String... parts) {
    throw new RuntimeException("No provider was found for datastore ("+getName()+") and the the url ("+getConnectionString()+")");
  }

  @Override
  public DataPathAbs getCurrentDataPath() {
    throw new RuntimeException("No provider was found for datastore ("+getName()+") and the the url ("+getConnectionString()+")");
  }

  @Override
  public DataPathAbs getQueryDataPath(String query) {
    throw new RuntimeException("No provider was found for datastore ("+getName()+") and the the url ("+getConnectionString()+")");
  }

  @Override
  public Integer getMaxWriterConnection() {
    throw new RuntimeException("No provider was found for datastore ("+getName()+") and the the url ("+getConnectionString()+")");
  }

  @Override
  public ProcessingEngine getProcessingEngine() {
    throw new RuntimeException("No provider was found for datastore ("+getName()+") and the the url ("+getConnectionString()+")");
  }

}
