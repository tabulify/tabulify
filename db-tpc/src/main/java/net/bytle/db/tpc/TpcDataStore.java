package net.bytle.db.tpc;

import net.bytle.db.database.DataStore;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPathAbs;
import net.bytle.db.spi.DataSystem;
import net.bytle.db.spi.ProcessingEngine;

import static net.bytle.db.tpc.TpcDataPath.CURRENT_WORKING_DIRECTORY;

public class TpcDataStore extends DataStore {


  private final TpcDataSetSystem tpcDataSystem;

  public TpcDataStore(String name, String url) {
    super(name, url);
    tpcDataSystem = new TpcDataSetSystem(this);
  }

  @Override
  public DataSystem getDataSystem() {
    return tpcDataSystem;
  }


  @Override
  public DataPath getDefaultDataPath(String... names) {
    if (names.length > 1) {
      throw new RuntimeException("There is two much names to define the path. It should be only one of word such as " + TpcdsModel.storeSalesTables);
    }
    String name = names[0];
    if (name.equals(CURRENT_WORKING_DIRECTORY)) {
      return getCurrentDataPath();
    } else {
      return getDataModel().getAndCreateDataPath(name);
    }
  }

  @Override
  public DataPath getTypedDataPath(String type, String... parts) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public DataPathAbs getCurrentDataPath() {
    return new TpcDataPath(this, CURRENT_WORKING_DIRECTORY);
  }

  @Override
  public DataPathAbs getQueryDataPath(String query) {
    throw new RuntimeException("The tpc data source does not support query");
  }

  @Override
  public Integer getMaxWriterConnection() {
    throw new RuntimeException("The tpc data source does not support writing only reading");
  }

  @Override
  public ProcessingEngine getProcessingEngine() {
    throw new RuntimeException("The tpc data source does not have a processing engine");
  }

  @Override
  public <T> T getObject(Object object, Class<T> clazz) {
    throw new RuntimeException("Not yet implemented");
  }

  private TpcdsModel tpcModel;

  public TpcdsModel getDataModel() {
    if (tpcModel == null) {
      this.tpcModel = TpcdsModel.of(this);
    }
    return tpcModel;
  }
}
