package com.tabulify.noop;

import com.tabulify.connection.Connection;
import com.tabulify.model.Constraint;
import com.tabulify.model.ForeignKeyDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataSystem;
import com.tabulify.transfer.TransferListener;
import com.tabulify.transfer.TransferProperties;
import net.bytle.type.MediaType;

import java.util.ArrayList;
import java.util.List;

public class NoOpDataSystem implements DataSystem {

  private final NoOpConnection noopConnection;

  public NoOpDataSystem(NoOpConnection noopConnection) {
    this.noopConnection = noopConnection;
  }

  @Override
  public Connection getConnection() {
    return this.noopConnection;
  }

  @Override
  public Boolean exists(DataPath dataPath) {
    return true;
  }

  @Override
  public boolean isContainer(DataPath dataPath) {
    return false;
  }

  @Override
  public void create(DataPath dataPath) {

  }

  @Override
  public void drop(DataPath dataPath) {

  }

  @Override
  public void delete(DataPath dataPath) {

  }

  @Override
  public void truncate(DataPath dataPath) {

  }

  @Override
  public <D extends DataPath> List<D> getChildrenDataPath(DataPath dataPath) {
    return new ArrayList<>();
  }

  @Override
  public Boolean isEmpty(DataPath queue) {
    return queue.getSize()==0;
  }

  @Override
  public boolean isDocument(DataPath dataPath) {
    return true;
  }

  @Override
  public String getString(DataPath dataPath) {
    throw new RuntimeException("This connection does not have any system");
  }

  @Override
  public TransferListener transfer(DataPath source, DataPath target, TransferProperties transferProperties) {
    throw new RuntimeException("This connection does not have any system and can't transfer");
  }

  @Override
  public <D extends DataPath> List<D> getDescendants(DataPath dataPath) {
    return new ArrayList<>();
  }

  @Override
  public <D extends DataPath> List<D> select(DataPath currentDataPath, String globNameOrPath, MediaType mediaType) {
    return new ArrayList<>();
  }

  @Override
  public List<ForeignKeyDef> getForeignKeysThatReference(DataPath dataPath) {
    return new ArrayList<>();
  }

  @Override
  public void dropConstraint(Constraint constraint) {

  }

  @Override
  public void truncate(List<DataPath> dataPaths) {

  }

  @Override
  public void dropNotNullConstraint(DataPath dataPath) {

  }

  @Override
  public void dropForce(DataPath dataPath) {

  }

  @Override
  public void execute(DataPath dataPath) {

  }
}
