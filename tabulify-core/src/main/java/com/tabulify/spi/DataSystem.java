package com.tabulify.spi;

import com.tabulify.connection.Connection;
import com.tabulify.model.*;
import com.tabulify.transfer.TransferListener;
import com.tabulify.transfer.TransferSourceTargetOrder;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MediaType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A data system
 * * a file system
 * * a relational database
 * * a memory system
 * <p></p>
 * The data system is where the data is stored
 * * a file system store them on the file system
 * * the relational database store them remotely on the server
 * * memory system store them in a map
 * <p></p>
 * See also Sisense that uses
 * JDBC for all data system:
 * <a href="https://documentation.sisense.com/docs/introduction-to-data-sources">...</a>
 * <p></p>
 * Example: ftp
 * <a href="https://documentation.sisense.com/docs/connecting-to-ftp">...</a>
 */
public interface DataSystem {


  /**
   * @return the system connection
   */
  Connection getConnection();

  /**
   * @return true if the data path exists
   */
  Boolean exists(DataPath dataPath);

  /**
   * @return true if this s a container of resources (data path). ie a file directory or a database schema
   */
  boolean isContainer(DataPath dataPath);

  /**
   * @param dataPath       - the data path to create
   * @param sourceDataPath - the source data path for the definition (maybe null)
   *                       Why we need the source?
   *                       Because a view can't be created by itself
   *                       (It needs a script, a script can be stored as a view)
   *                       while a table can be created from a definition
   *                       A table can also be created from a sql file with a `create`/`select` statement
   * @param sourceTargets  - the source target mapping in case of foreign key copy (maybe null)
   */
  void create(DataPath dataPath, DataPath sourceDataPath, Map<DataPath, DataPath> sourceTargets);


  /**
   * @param dataPaths      - the data paths to drop
   * @param dropAttributes - the attributes
   *                       <p>
   *                       Why a list of data path?
   *                       Because, database  may not allow a drop even in good drop order if a foreign key exists
   *                       Example:
   *                       Caused by: org.postgresql.util.PSQLException: ERROR: cannot drop a table referenced in a foreign key constraint
   *                       Detail: Table "f_sales" references "d_date".
   *                       Hint: Drop table "f_sales" at the same time, or use DROP ... CASCADE.
   */
  void drop(List<DataPath> dataPaths, Set<DropTruncateAttribute> dropAttributes);


  /**
   * Truncate data resources
   * Why a list and not one by one?
   * Because some database does not allow it
   * Caused by: org.postgresql.util.PSQLException: ERROR: cannot truncate a table referenced in a foreign key constraint
   *   Detail: Table "f_sales" references "d_date".
   *   Hint: Truncate table "f_sales" at the same time, or use TRUNCATE ... CASCADE.
   */
  void truncate(List<DataPath> dataPaths, Set<DropTruncateAttribute> truncateAttributes);


  /**
   * @return the children
   */
  <D extends DataPath> List<D> getChildrenDataPath(DataPath dataPath);


  /**
   * @return if the target is empty, no record
   * In a file system, we check the records because it may have metadata such as heading
   * in a sql system, we check the count
   */
  Boolean isEmpty(DataPath dataPath);


  /**
   * @return true if the data path locate a document
   * <p>
   * The opposite is {@link #isContainer(DataPath)}
   */
  boolean isDocument(DataPath dataPath);


  /**
   * @return the content of a data path in a string format
   */
  String getContentAsString(DataPath dataPath);

  /**
   * Copy the source to the target on the same data store
   * * cp on Os
   * * insert/create from select
   */
  TransferListener transfer(TransferSourceTargetOrder transferOrder);


  /**
   * @param dataPath the ancestor data path
   * @return the descendants of the data path
   */
  <D extends DataPath> List<D> getDescendants(DataPath dataPath);

  /**
   * @param currentDataPath a data path container representing the current path (a directory, a schema or a catalog)
   * @param globNameOrPath  a glob path or that filters the descendant data path returned
   * @param mediaType       the media type
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
   *
   * @param constraint - the constraint to drop
   */
  void dropConstraint(Constraint constraint);


  /**
   * @return a SQL Vendor type (ie our own data type definition)
   * The main advantage between a JDBC data type and the Vendor is that it defines the class to use
   * <p>
   */
  SqlDataTypeVendor getSqlDataTypeVendor(KeyNormalizer typeName, int typeCode);

  /**
   * Drop the not null constraint
   */
  void dropNotNullConstraint(DataPath dataPath);


  /**
   * In a transfer when the target resource does not exist,
   * we need to create it. System may have constraints such as
   * sql that does not allow a digit as first letter.
   * This function will return a data path with a valid name to create a target resource
   * from a source resource.
   * Why we return the data path and not just a name? Because in a transfer between systems
   * The target connection may want to set attribute/variable on the created resource
   * For example, from sql to file system, if the default is a csv, we may set add or not a header.
   *
   * @param sourceDataPath       - the source data path
   * @param targetMediaType      - the media type to create (You may want to create a view instead of a table, the connection needs to validate that it supports it)
   * @param targetParentDataPath - the parent of the target (by default, the {@link Connection#getCurrentDataPath() current data path} ie may be a directory in a connection
   * @return the target data path
   */
  DataPath getTargetFromSource(DataPath sourceDataPath, MediaType targetMediaType, DataPath targetParentDataPath);

  /**
   * @param name - a name
   * @return a valid name
   * For instance, in sql, you can't have a name that starts with a number or have a point
   * This function will correct it and returns a valid name
   * Use normally in {@link #getTargetFromSource(DataPath, MediaType, DataPath)}
   * but also for the creation of script or automated name
   */
  String toValidName(String name);


  /**
   * @return the container media type for the system (ie a dir for a file system, a schema for sql)
   */
  MediaType getContainerMediaType();

  /**
   * A plugin point to update the data type
   */
  void dataTypeBuildingMain(SqlDataTypeManager sqlDataTypeManager);

  /**
   * @return the physical size in bytes of the resource
   */
  Long getSize(DataPath dataPath);

  /**
   * @return the type or a subset of the type for the system (called vendor in JDBC)
   */
  Set<SqlDataTypeVendor> getSqlDataTypeVendors();

  /**
   * @return the unique identifier for a type
   */
  SqlTypeKeyUniqueIdentifier getSqlTypeKeyUniqueIdentifier();
}
