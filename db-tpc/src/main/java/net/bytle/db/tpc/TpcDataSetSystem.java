package net.bytle.db.tpc;

import net.bytle.db.database.DataStore;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataSetSystem;
import net.bytle.db.stream.SelectStream;
import net.bytle.regexp.Globs;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TpcDataSetSystem extends DataSetSystem {

  // A property in the datastore that give the scale used
  // The size of the generated data in Gb (works only for tpc schema)
  public static final String SCALE = "scale";





  @Override
  public Boolean exists(DataPath dataPath) {
    assert dataPath != null : "A data path should not be null";
    TpcDataPath tpcDataPath = (TpcDataPath) dataPath;
    return tpcDataPath.getDataStore().getDataModel().getAndCreateDataPath(dataPath.getName()) != null;
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
  public boolean isContainer(DataPath dataPath) {
    return false;
  }



  @Override
  public List<DataPath> getChildrenDataPath(DataPath dataPath) {
    if (dataPath.getPath().equals(TpcDataPath.CURRENT_WORKING_DIRECTORY)) {
      return ((TpcDataStore) dataPath.getDataStore()).getDataModel().getAndCreateDataPaths();
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
    TpcDataPath  tpcDataPath = (TpcDataPath) dataPath;
    return tpcDataPath.getDataStore().getDataModel().getAndCreateDataPaths().stream()
      .filter(
        s -> s.getDataDef()
          .getForeignKeys().stream()
          .filter(d -> d.getForeignPrimaryKey().getDataDef().getDataPath().equals(dataPath))
          .count() > 0
      )
      .collect(Collectors.toList());
  }

  @Override
  public DataStore createDataStore(String name, String url) {
    return new TpcDataStore(name,url, this);
  }


}
