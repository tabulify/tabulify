package net.bytle.db.tpc;

import net.bytle.db.connection.Connection;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataSetSystemAbs;
import net.bytle.regexp.Glob;
import net.bytle.type.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TpcDataSetSystem extends DataSetSystemAbs {


  private final TpcConnection tpcConnection;

  public TpcDataSetSystem(TpcConnection tpcConnection) {
    super(tpcConnection);
    this.tpcConnection = tpcConnection;
  }

  @Override
  public Connection getConnection() {
    return this.tpcConnection;
  }

  @Override
  public Boolean exists(DataPath dataPath) {
    assert dataPath != null : "A data path should not be null";
    TpcDataPath tpcDataPath = (TpcDataPath) dataPath;
    return tpcDataPath.getConnection().getDataModel().getAndCreateDataPath(dataPath.getName()) != null;
  }




  @Override
  public boolean isContainer(DataPath dataPath) {
    return false;
  }


  @SuppressWarnings("unchecked")
  @Override
  public List<DataPath> getChildrenDataPath(DataPath dataPath) {
    if (dataPath.getRelativePath().equals(TpcDataPath.CURRENT_WORKING_DIRECTORY_NAME)) {
      return ((TpcConnection) dataPath.getConnection()).getDataModel().getAndCreateDataPaths();
    } else {
      return new ArrayList<>();
    }
  }


  @Override
  public Boolean isEmpty(DataPath dataPath) {
    return false;
  }

  @Override
  public Long count(DataPath dataPath) {
    if (isDocument(dataPath)) {
      /**
       * TODO: The size is {@link #SCALE} dependent
       *
       */
      return 0L;
    } else {
      return (long) getDescendants(dataPath).size();
    }
  }


  @Override
  public boolean isDocument(DataPath dataPath) {
    return !dataPath.getRelativePath().equals(TpcDataPath.CURRENT_WORKING_DIRECTORY_NAME);
  }


  @Override
  public List<DataPath> getDescendants(DataPath dataPath) {
    return getChildrenDataPath(dataPath);
  }



  @SuppressWarnings("unchecked")
  @Override
  public List<DataPath> select(DataPath dataPath, String globNameOrPath, MediaType mediaType) {

    Glob glob = Glob.createOf(globNameOrPath);
    return getChildrenDataPath(dataPath).stream()
      .filter(d -> glob.matches(d.getRelativePath()))
      .collect(Collectors.toList());
  }

  @Override
  public List<ForeignKeyDef> getForeignKeysThatReference(DataPath dataPath) {
    TpcDataPath tpcDataPath = (TpcDataPath) dataPath;
    return tpcDataPath.getConnection().getDataModel().getAndCreateDataPaths().stream()
      .flatMap(s -> s.getOrCreateRelationDef()
        .getForeignKeys().stream())
      .filter(d -> d.getForeignPrimaryKey().getRelationDef().getDataPath().equals(dataPath))
      .collect(Collectors.toList());
  }


}
