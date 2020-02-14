package net.bytle.db.tpc;

import net.bytle.db.database.DataStore;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.ProcessingEngine;

import static net.bytle.db.tpc.TpcDataPath.CURRENT_WORKING_DIRECTORY;

public class TpcDataStore extends DataStore {

  private final TpcDataSetSystem tpcDataSetSystem;

  public TpcDataStore(String name, String url, TpcDataSetSystem tpcDataSetSystem) {
    super(name, url);
    this.tpcDataSetSystem = tpcDataSetSystem;
  }

  @Override
  public TpcDataSetSystem getDataSystem() {
    return tpcDataSetSystem;
  }

  @Override
  public DataPath getDataPath(String... names) {
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
  public DataPath getCurrentDataPath() {
    return new TpcDataPath(this, CURRENT_WORKING_DIRECTORY);
  }

  @Override
  public DataPath getQueryDataPath(String query) {
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

  private TpcdsModel tpcModel;

  public TpcdsModel getDataModel() {
    if (tpcModel == null) {
      this.tpcModel = TpcdsModel.of(this);
    }
    return tpcModel;
  }
}
