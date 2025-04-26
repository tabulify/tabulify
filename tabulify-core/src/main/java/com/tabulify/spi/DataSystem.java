package com.tabulify.spi;

import net.bytle.type.MediaType;
import com.tabulify.connection.Connection;
import com.tabulify.model.Constraint;
import com.tabulify.model.ForeignKeyDef;
import com.tabulify.transfer.TransferListener;
import com.tabulify.transfer.TransferProperties;

import java.util.List;

/**
 * A data system
 *   * a file system
 *   * a relational database
 *   * a memory system
 *<p></p>
 * The data system is where the data is stored
 *   * a file system store them on the file system
 *   * the relational database store them remotely on the server
 *   * memory system store them in a map
 *<p></p>
 * See also Sisense that uses
 * JDBC for all data system:
 * <a href="https://documentation.sisense.com/docs/introduction-to-data-sources">...</a>
 * <p></p>
 * Example: ftp
 * <a href="https://documentation.sisense.com/docs/connecting-to-ftp">...</a>
 */
public interface DataSystem {


  Connection getConnection();

  Boolean exists(DataPath dataPath);

  boolean isContainer(DataPath dataPath);

  void create(DataPath dataPath);

  void drop(DataPath dataPath);

  void delete(DataPath dataPath);

  /**
   * An utility function that uses{@link #truncate}
   */
  void truncate(DataPath dataPath);



  <D extends DataPath> List<D> getChildrenDataPath(DataPath dataPath);


  /**
   *
   * @return if the target is empty, no record
   * In a file system, we check the records because it may have metadata such as heading
   * in a sql system, we check the count
   */
  Boolean isEmpty(DataPath queue);



  /**
   * @return true if the data path locate a document
   * <p>
   * The opposite is {@link #isContainer(DataPath)}
   */
  boolean isDocument(DataPath dataPath);


  /**
   * @return the content of a data path in a string format
   */
  String getString(DataPath dataPath);

  /**
   * Copy the source to the target on the same data store
   *   * cp on Os
   *   * insert/create from select
   *
   */
  TransferListener transfer(DataPath source, DataPath target, TransferProperties transferProperties);


  /**
   * @param dataPath the ancestor data path
   * @return the descendants of the data path
   */
  <D extends DataPath> List<D> getDescendants(DataPath dataPath);

  /**
   * @param currentDataPath a data path container representing the current path (a directory, a schema or a catalog)
   * @param globNameOrPath  a glob path or that filters the descendant data path returned
   * @param mediaType     the media type
   * @return the selected data paths representing sql tables, schema or files
   */
  <D extends DataPath> List<D> select(DataPath currentDataPath, String globNameOrPath, MediaType mediaType);

  /**
   * @param dataPath the data path
   * @return the foreign keys that references the data path primary key (known also as exported keys)
   */
  List<ForeignKeyDef> getForeignKeysThatReference(DataPath dataPath);


  /**
   * Drop a {@link Constraint}
   */
  void dropConstraint(Constraint constraint);

  /**
   * A full sql truncate statement may have the table and all its reference
   */
  void truncate(List<DataPath> dataPaths);

  /**
   * Drop the not null constraint
   */
  void dropNotNullConstraint(DataPath dataPath);

  /**
   * Drop with force (ie drop constraint)
   * @param dataPath
   * The function is here because not all datastore
   * supports the drop of constraints (ie sqlite for instance)
   */
  void dropForce(DataPath dataPath);


  /**
   * Execute a script data path
   * TODO: Te be able to cancel, we could also get an execution object with the start, cancel and close method
   */
  void execute(DataPath dataPath);

  /**
   * In a transfer when the target resource does not exist,
   * we need to create it. System may have constraints such as
   * sql that does not allow a digit as first letter.
   * This function will return a data path with a valid name to create a target resource
   * from a source resource.
   * Why we return the data path and not just a name? Because in transfer between system
   * The target connection may want to set attribute/variable on the created resource
   * For example, from sql to file system, if the default is a csv, we may set add or not a header.
   * @param sourceDataPath - the source data path
   * @return the data path
   */
  DataPath getTargetFromSource(DataPath sourceDataPath);

  /**
   * @param name - a name
   * @return a valid name
   * For instance, in sql, you can't have a name that starts with a number or have a point
   * This function will correct it and returns a valid name
   * Use normally in {@link #getTargetFromSource(DataPath)}
   * but also for the creation of script or automated name
   */
  String toValidName(String name);

}
