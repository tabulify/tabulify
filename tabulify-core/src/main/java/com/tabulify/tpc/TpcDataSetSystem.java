package com.tabulify.tpc;

import com.tabulify.connection.Connection;
import com.tabulify.model.ForeignKeyDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataSetSystemAbs;
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
      return ((TpcConnection) dataPath.getConnection()).getDataModel().createDataPaths();
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
  public <D extends DataPath> List<D> getDescendants(DataPath dataPath) {
    //noinspection unchecked
    return (List<D>) getChildrenDataPath(dataPath);
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
    return tpcDataPath.getConnection().getDataModel().createDataPaths().stream()
      .flatMap(s -> s.getOrCreateRelationDef()
        .getForeignKeys().stream())
      .filter(d -> d.getForeignPrimaryKey().getRelationDef().getDataPath().equals(dataPath))
      .collect(Collectors.toList());
  }




}
