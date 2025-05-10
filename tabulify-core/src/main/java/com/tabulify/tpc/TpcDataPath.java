package com.tabulify.tpc;

import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataPathAbs;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferProperties;
import com.teradata.tpcds.Table;
import net.bytle.type.MediaTypes;

import java.util.Collections;
import java.util.List;

public class TpcDataPath extends DataPathAbs {


  public static final String CURRENT_WORKING_DIRECTORY_NAME = "/";
  public static final String SEPARATOR = ".";

  private final TpcConnection connection;
  private final String name;


  public TpcDataPath(TpcConnection tpcConnection, String name) {
    super(tpcConnection, name, MediaTypes.SQL_RELATION);
    this.connection = tpcConnection;
    this.name = name;
  }

  public static TpcDataPath of(TpcConnection dataStore, String name) {
    return new TpcDataPath(dataStore, name);
  }

  @Override
  public TpcConnection getConnection() {
    return this.connection;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public List<String> getNames() {
    return Collections.singletonList(name);
  }

  @Override
  public String getRelativePath() {
    return getName();
  }

  @Override
  public String getAbsolutePath() {
    return getName();
  }

  @Override
  public Long getSize() {
    TpcLog.LOGGER_TPC.fine("The size operation is not yet implemented for TPC");
    return null;

  }

  @Override
  public Long getCount() {
    TpcLog.LOGGER_TPC.fine("The count operation is not yet implemented for TPC");
    return null;
  }

  @Override
  public InsertStream getInsertStream(DataPath source, TransferProperties transferProperties) {
    throw new RuntimeException("A TPC data resource is a read-only data resources. You can't insert and use it as a target.");
  }

  @Override
  public SelectStream getSelectStream() {

    return TpcdsSelectStream.create(this);

  }

  @Override
  public DataPath getParent() {
    return this.getConnection().getCurrentDataPath();
  }

  @Override
  public boolean hasHeaderInContent() {
    return false;
  }

  @Override
  public DataPath getSibling(String name) {
    return this.connection.getDataModel().getAndCreateDataPath(name);
  }

  @Override
  public DataPath getChild(String name) {
    if (this.name.equals(CURRENT_WORKING_DIRECTORY_NAME)) {
      return this.connection.getDataModel().getAndCreateDataPath(name);
    } else {
      throw new RuntimeException("You can't get a child from the table (" + this.name + ")");
    }
  }

  @Override
  public DataPath resolve(String stringPath) {
    return getSibling(stringPath);
  }


  @Override
  public DataPath getChildAsTabular(String name) {
    return getChild(name);
  }


  @Override
  public DataPath getSelectStreamDependency() {
    DataPath dependency = null;
    if (!this.getName().toLowerCase().startsWith("s_")) {
      Table table = Table.getTable(this.getName());
      if (table.isChild()) {
        dependency = getSibling(table.getParent().getName());
      }
    }
    return dependency;
  }
}
