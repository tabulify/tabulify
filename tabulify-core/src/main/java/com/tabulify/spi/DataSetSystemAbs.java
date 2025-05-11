package com.tabulify.spi;

import com.tabulify.connection.Connection;
import com.tabulify.model.Constraint;
import com.tabulify.model.ForeignKeyDef;
import com.tabulify.transfer.TransferListener;
import com.tabulify.transfer.TransferProperties;

import java.util.List;

/**
 * Data Set = Source only data store
 * <p></p>
 * A data set system does not have all table system because it's only a read system
 * This abstract method takes care of this
 */
public abstract class DataSetSystemAbs extends DataSystemAbs implements DataSystem {


  public DataSetSystemAbs(Connection connection) {
    super(connection);
  }

  public abstract Boolean exists(DataPath dataPath);


  public void create(DataPath dataPath) {
    throw new RuntimeException("A data set cannot create a data path. It can only read it");
  }


  public void drop(DataPath dataPath) {
    throw new RuntimeException("A data set cannot drop a data path. It can only read it");
  }

  public void delete(DataPath dataPath) {
    throw new RuntimeException("A data set cannot delete a data path. It can only read it");
  }

  public void truncate(List<DataPath> dataPaths) {
    throw new RuntimeException("A data set cannot truncate a data path. It can only read it");
  }

  @Override
  public void dropConstraint(Constraint constraint) {
    throw new RuntimeException("A data set system does not have the notion of constraint");
  }


  public abstract Boolean isEmpty(DataPath queue);

  public abstract Long count(DataPath dataPath);

  /**
   * @return true if the data path locate a document
   * <p>
   * The opposite is {@link #isContainer(DataPath)}
   */
  public abstract boolean isDocument(DataPath dataPath);


  /**
   * @return the content of a data path in a string format
   */
  @Override
  public String getString(DataPath dataPath) {
    throw new RuntimeException("Not implemented had this time");
  }

  @Override
  public TransferListener transfer(DataPath source, DataPath target, TransferProperties transferProperties) {
    throw new RuntimeException("A data set cannot copy (write) data. It can only read them");
  }


  /**
   * @param dataPath the data path
   * @return data paths that references the data path primary key (via foreign keys)
   */
  public abstract List<ForeignKeyDef> getForeignKeysThatReference(DataPath dataPath);


  @Override
  public void execute(DataPath dataPath) {
    throw new UnsupportedOperationException("A data set system does not support execution");
  }



}
