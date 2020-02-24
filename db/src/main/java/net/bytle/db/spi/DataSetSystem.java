package net.bytle.db.spi;

import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.transfer.TransferProperties;

import java.util.List;

/**
 * A data set system does not have all table system because it's only a read system
 * This abstract method takes care of this
 */
public abstract class DataSetSystem extends TableSystem {




  public abstract Boolean exists(DataPath dataPath);

  public abstract SelectStream getSelectStream(DataPath dataPath);


  public void create(DataPath dataPath) {
    throw new RuntimeException("A data set cannot create a data path. It can only read it");
  }


  public void drop(DataPath dataPath) {
    throw new RuntimeException("A data set cannot drop a data path. It can only read it");
  }

  public void delete(DataPath dataPath) {
    throw new RuntimeException("A data set cannot delete a data path. It can only read it");
  }

  public void truncate(DataPath dataPath) {
    throw new RuntimeException("A data set cannot truncate a data path. It can only read it");
  }



  public InsertStream getInsertStream(DataPath dataPath) {
    throw new RuntimeException("A data set cannot insert into a data path. It can only read it");
  }




  @Override
  public void move(DataPath source, DataPath target, TransferProperties transferProperties) {
    throw new RuntimeException("A data set cannot move its data paths. It can only read them");
  }


  public abstract Boolean isEmpty(DataPath queue);

  public abstract long size(DataPath dataPath);

  /**
   * @param dataPath
   * @return true if the data path locate a document
   * <p>
   * The opposite is {@link #isContainer(DataPath)}
   */
  public abstract boolean isDocument(DataPath dataPath);


  /**
   * @param dataPath
   * @return the content of a data path in a string format
   */
  @Override
  public String getString(DataPath dataPath) {
    throw new RuntimeException("Not implemented had this time");
  }

  @Override
  public TransferListener copy(DataPath source, DataPath target, TransferProperties transferProperties) {
    throw new RuntimeException("A data set cannot copy (write) data. It can only read them");
  }

  @Override
  public TransferProperties insert(DataPath source, DataPath target, TransferProperties transferProperties) {
    throw new RuntimeException("A data set cannot insert into its data paths. It can only read them");
  }

  /**
   * @param dataPath the ancestor data path
   * @return the descendants of the data path
   */
  public abstract List<DataPath> getDescendants(DataPath dataPath);

  /**
   * @param dataPath a data path container (a directory, a schema or a catalog)
   * @param glob     a glob that filters the descendant data path returned
   * @return the descendant data paths representing sql tables, schema or files
   */
  public abstract List<DataPath> getDescendants(DataPath dataPath, String glob);

  /**
   * @param dataPath the data path
   * @return data paths that references the data path primary key (via foreign keys)
   */
  public abstract List<DataPath> getReferences(DataPath dataPath);



}
