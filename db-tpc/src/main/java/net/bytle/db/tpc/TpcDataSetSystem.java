package net.bytle.db.tpc;

import net.bytle.db.database.DataStore;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataSetSystem;
import net.bytle.db.spi.TableSystemProvider;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.uri.DataUri;
import net.bytle.regexp.Globs;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TpcDataSetSystem extends DataSetSystem {

  private static TpcDataSetSystem tpcDataSetSystem;
  private final TpcTableSystemProvider tpcTableSystemProvider;
  private TpcdsModel tpcModel;
  private final DataStore dataStore;


  private TpcDataSetSystem(TpcTableSystemProvider tpcTableSystemProvider, DataStore dataStore) {
    this.tpcTableSystemProvider = tpcTableSystemProvider;
    this.dataStore = dataStore;
  }

  public static TpcDataSetSystem of(TpcTableSystemProvider tpcTableSystemProvider, DataStore dataStore) {
    if (tpcDataSetSystem == null) {
      tpcDataSetSystem = new TpcDataSetSystem(tpcTableSystemProvider,dataStore);
    }
    return tpcDataSetSystem;
  }

  public TpcdsModel getDataModel() {
    if (tpcModel == null) {
      this.tpcModel = TpcdsModel.of(this);
    }
    return tpcModel;
  }

  @Override
  public DataPath getDataPath(DataUri dataUri) {

    return getDataPath(dataUri.getPath());

  }

  @Override
  public DataPath getDataPath(String... names) {
    DataPath dataPath = this.getDataModel().getAndCreateDataPath(names[0]);
    // Case when it's the working directory
    if (dataPath == null) {
      dataPath = TpcDataPath.of(this, names[0]);
    }
    return dataPath;
  }


  @Override
  public Boolean exists(DataPath dataPath) {
    assert dataPath != null : "A data path should not be null";
    return tpcModel.getAndCreateDataPath(dataPath.getName()) != null;
  }

  /**
   * Select stream may be dependent, you c
   *
   * @param dataPath
   * @return a select stream
   */
  @Override
  public SelectStream getSelectStream(DataPath dataPath) {

    return TpcdsSelectStream.of(dataPath);

  }

  @Override
  public DataStore getDataStore() {
    return dataStore;
  }




  @Override
  public boolean isContainer(DataPath dataPath) {
    return false;
  }

  @Override
  public String getProductName() {
    return TpcTableSystemProvider.TPCDS_SCHEME;
  }


  @Override
  public TableSystemProvider getProvider() {

    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public List<DataPath> getChildrenDataPath(DataPath dataPath) {
    if (dataPath.getPath().equals(TpcDataPath.CURRENT_WORKING_DIRECTORY)) {
      return this.tpcModel.getAndCreateDataPaths();
    } else {
      return new ArrayList<>();
    }
  }


  @Override
  public Boolean isEmpty(DataPath dataPath) {
    return false;
  }

  @Override
  public Integer size(DataPath dataPath) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public boolean isDocument(DataPath dataPath) {
    if (dataPath.getPath().equals(TpcDataPath.CURRENT_WORKING_DIRECTORY)) {
      return false;
    } else {
      return true;
    }
  }

  @Override
  public DataPath getCurrentPath() {
    return getDataPath(".");
  }

  @Override
  public List<DataPath> getDescendants(DataPath dataPath) {
    return getChildrenDataPath(dataPath);
  }

  @Override
  public List<DataPath> getDescendants(DataPath dataPath, String glob) {
    String pattern = Globs.toRegexPattern(glob);
    return getChildrenDataPath(dataPath).stream()
      .filter(d -> d.getPath().matches(pattern))
      .collect(Collectors.toList());
  }

  @Override
  public List<DataPath> getReferences(DataPath dataPath) {
    return getDataModel().getAndCreateDataPaths().stream()
      .filter(
        s -> s.getDataDef()
          .getForeignKeys().stream()
          .filter(d -> d.getForeignPrimaryKey().getDataDef().getDataPath().equals(dataPath))
          .count() > 0
      )
      .collect(Collectors.toList());
  }


  @Override
  public void close() {

  }
}
