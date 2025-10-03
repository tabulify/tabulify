package com.tabulify.noop;

import com.tabulify.model.Constraint;
import com.tabulify.model.ForeignKeyDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataSystemAbs;
import com.tabulify.spi.DropTruncateAttribute;
import com.tabulify.transfer.TransferListener;
import com.tabulify.transfer.TransferSourceTargetOrder;
import net.bytle.type.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NoOpDataSystem extends DataSystemAbs {


  public NoOpDataSystem(NoOpConnection noopConnection) {
    super(noopConnection);
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
  public void create(DataPath dataPath, DataPath sourceDataPath, Map<DataPath, DataPath> sourceTargets) {

  }

  @Override
  public void drop(List<DataPath> dataPaths, Set<DropTruncateAttribute> dropAttributes) {

  }

  @Override
  public void truncate(List<DataPath> dataPaths, Set<DropTruncateAttribute> truncateAttributes) {

  }


  @Override
  public <D extends DataPath> List<D> getChildrenDataPath(DataPath dataPath) {
    return new ArrayList<>();
  }

  @Override
  public Boolean isEmpty(DataPath queue) {
    return queue.getSize() == 0;
  }

  @Override
  public boolean isDocument(DataPath dataPath) {
    return true;
  }

  @Override
  public String getContentAsString(DataPath dataPath) {
    throw new RuntimeException("This connection does not have any system");
  }

  @Override
  public TransferListener transfer(TransferSourceTargetOrder transferOrder) {
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
  public void dropNotNullConstraint(DataPath dataPath) {

  }


  @Override
  public MediaType getContainerMediaType() {
    throw new UnsupportedOperationException("The noop connection does not support container resources (directory)");
  }

}
