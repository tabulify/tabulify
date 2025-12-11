package com.tabulify.spi;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.AttributeEnum;
import com.tabulify.connection.Connection;
import com.tabulify.engine.StreamDependencies;
import com.tabulify.model.RelationDef;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferPropertiesSystem;
import com.tabulify.uri.DataUriNode;
import com.tabulify.dag.Dependency;
import com.tabulify.exception.NoParentException;
import com.tabulify.exception.NotFoundException;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.MediaType;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A data path is the in-memory representation of a data resource
 * <p></p>
 * It may be used to locate a data container (such as a file or a table)
 * in a data system (file system, relational database).
 */
public interface DataPath extends Comparable<DataPath>, Dependency, StreamDependencies, Meta {

  /**
   * @return the connection of this data path
   */
  Connection getConnection();

  /**
   * A name is the name identifier.
   * <p></p>
   * A name is generally the last part of the path
   * <p></p>
   * It should not be null even for a data path without a path (ie anonymous)
   * Generate a name with a prefix and a generated id in this case
   * <p></p>
   * A File system may add structure information in the name in the suffix.
   * <p></p>
   * Example:
   * * data.csv
   * * or data--datagen.csv
   * The name without the suffix is called a  simplified name
   * The name of the root path (ie /) is the empty string
   *
   * @return the name of the resource
   */
  String getName();

  /**
   * The name without any suffix used to match database logical name
   * <p>
   * Example in `data.csv`, the simplified name is `data`
   * but you may choose another one to map to another table
   *
   * @return A logical name (simplified name)
   */
  String getLogicalName();

  /**
   * @return the names of the path
   */
  List<String> getNames();

  /**
   * @return the relative path in the data system
   * This is an identifier for all static path type(ie not for the runtime resource)
   * The real identifier is {@link #getId}
   */
  String getCompactPath();

  /**
   * @return the id
   * It should be equivalent to {@link #toDataUri()})
   * <p>
   * Identifier is also used in the {@link Dependency#getId() dependency}
   * in order to determine object equality
   */
  String getId();

  /**
   * @return a {@link DataDefManifest} object created from a metadata data store (ie wrapper around {@link #createRelationDef()}
   * <p>
   * The function {@link #createEmptyRelationDef()} create only an empty object
   * <p>
   * If the {@link RelationDef} is empty at access time, it's:
   * * a {@link DataPath} that does not exist
   * * or an {@link #isRuntime()} executable. If it's an executable, you can get the data def at {@link SelectStream#getRuntimeRelationDef()}
   */
  RelationDef getOrCreateRelationDef();


  /**
   * @param name - the sibling name
   * @return a sibling (ie on the path `/a/c`, the sibling `b` would be `/a/b`
   * <p>
   * Example with a data path equivalent to /foo/bar and foo as name, we get a DataPath of /foo/foo
   * Equivalent to the {@link java.nio.file.Path#resolveSibling(String)}
   */
  DataPath getSibling(String name);

  /**
   * @param name      - a child name
   * @param mediaType - the media type
   * @return a child (ie on the path `/a/c`, the child `b` would be `/a/c/b`
   * <p>
   * This is the equivalent to the {@link Path#resolve(String)}
   * but where:
   * * you can't use ".." and "."
   * * you can use only one argument
   */
  @SuppressWarnings("GrazieInspection")
  DataPath resolve(String name, MediaType mediaType);

  /**
   * @return the dependencies
   */
  Set<? extends Dependency> getDependencies();

  /**
   * @param description the description of the data path
   * @return the path
   */
  DataPath setComment(String description);

  /**
   * @return the description of the data path
   */
  String getComment();


  /**
   * The media type defines the content type:
   * * for a file system (web): mime type (ie csv, ...)
   * * for a memory system: list / queue / gen
   * * for a relational system: table, view, query
   * <p></p>
   * The name was taken from the term `Internet media type`
   * from the mime specification.
   * <a href="http://www.iana.org/assignments/media-types/media-types.xhtml">MediaType</a>
   *
   * @return the media type
   */
  MediaType getMediaType();

  /**
   * Get a select stream over this resource safely
   * If this is a runtime, the runtime is {@link #execute() executed}
   */
  SelectStream getSelectStreamSafe();

  /**
   * When processing a stream, another processing should have run before
   * This function returns the data path that would generate a dependency stream
   * <p>
   * This is the case in TPC-DS where the `returns` are generated from the `sales`
   *
   * @return the path that depends on this path for a select
   */
  DataPath getSelectStreamDependency() throws NotFoundException;

  /**
   * @return an {@link RelationDef} object initialized with the metadata of the underlying {@link DataSystem system} metastore
   * Overwrite the actual relation def if any
   * <p>
   * Use {@link #createEmptyRelationDef()} if you don't want to get the  {@link RelationDef} from the underlying {@link DataSystem system}
   * <p>
   * This is the method that is overridden by implementation.
   * The relation def is then retrieved with {@link #getOrCreateRelationDef()}
   */
  RelationDef createRelationDef();

  /**
   * @return Overwrite if any a relation def and returns an empty {@link RelationDef} object
   * <p>
   * Used:
   * * when loading {@link #mergeDataDefinitionFromYamlMap(Map)}
   * * when you don't want to get the  {@link RelationDef} from the underlying {@link DataSystem system}
   * * reset the relation def
   * <p>
   */
  RelationDef createEmptyRelationDef();

  /**
   * @return a data def or null
   */
  RelationDef getRelationDef();

  /**
   * @return the data uri
   */
  DataUriNode toDataUri();

  /**
   * @return the absolute path string from the data system (not the connection)
   * ie the full qualified path for a file system
   */
  String getAbsolutePath();


  byte[] getByteDigest(String digestAlgo) throws NoSuchAlgorithmException, IOException;


  DataPath mergeDataPathAttributesFrom(DataPath source);


  /**
   * Merge the data definition
   * The data definition is first created with the metadata store {@link #createRelationDef()}
   * then merged
   */
  DataPath mergeDataDefinitionFromYamlMap(Map<KeyNormalizer, ?> document);


  /**
   * Meta = {@link #getAttributes() DataPath Properties}  and {@link #getOrCreateRelationDef() Structure}
   *
   * @param sourceDataPath the source path
   * @return the path
   */
  DataPath mergeDataDefinitionFrom(DataPath sourceDataPath);

  /**
   * @param sourceDataPath  the source data path
   * @param sourceTargetMap - the source target in case of cross transfer or no memory store
   * @return the path
   */
  DataPath mergeDataDefinitionFrom(DataPath sourceDataPath, Map<DataPath, DataPath> sourceTargetMap);

  /**
   * Set the simple name
   * This is used inside data definition file
   * to be able to use another name than the file name
   *
   * @param logicalName the logical name
   * @return the path
   */
  DataPath setLogicalName(String logicalName);

  /**
   * @return true if the data path is a runtime resource
   */
  boolean isRuntime();


  /**
   * The size of the data path
   *
   * @return the size on disk in byte
   */
  Long getSize();

  /**
   * Count of the content
   * * ie count of rows for table
   * * count of tables for schema
   * <p>
   * Count should be on the data path because
   * this is a single attribute and when you summarize
   * or pass it along a stream, this is more convenient
   *
   * @return the number of record
   */
  Long getCount();

  /**
   * The insert stream permits to insert data into the data resources
   * <p>
   * The insert stream may need to know the source to be created
   * Example: if there is 3 columns in the source and 5 in the target,
   * the sql created will just be on 3 columns.
   *
   * @param transferProperties the transfer properties
   * @return the object for chaining
   */
  InsertStream getInsertStream(DataPath source, TransferPropertiesSystem transferProperties);

  /**
   * Utility function with no source and transfer properties definition
   *
   * @return the insert stream
   */
  InsertStream getInsertStream();

  /**
   * Return a select stream to get data via a pointer
   * If there is an error while getting the select stream
   * such as security, a selection exception is thrown.
   * <p>
   * By default, you would throw a runtime error
   * but for a total computation such as a count that you want to print,
   * you may catch this exception and go further
   *
   * @return the select stream
   */
  SelectStream getSelectStream() throws SelectException;


  InsertStream getInsertStream(TransferPropertiesSystem transferProperties);

  DataPath getParent() throws NoParentException;


  /**
   * @return true if the file headers are stored alongside the content
   * It's important for transfer operation.
   * You can use block operation to append a source file to an existing file if it has no header.
   */
  boolean hasHeaderInContent();

  /**
   * If this data path has a code data path or null
   * If the code is stored in a string, a memory data path is used
   */
  DataPath getExecutableDataPath();


  /**
   * @return if the structure of this data path is fixed
   * (ie if this is an executable, a view or not)
   * If this is the case, we can't make assumption on the nullability of column for instance.
   */
  SchemaType getSchemaType();

  /**
   * Utility function that gets a child with the default media type
   * ie {@link #resolve(String, MediaType)} with a null media type
   */
  DataPath resolve(String name);


  /**
   * @return the last access time if known (ie atime)
   */
  Timestamp getAccessTime();

  /**
   * @return the last time that the content was changed (ie update time, ie utime)
   */
  Timestamp getUpdateTime();


  /**
   * @return the creation time (ie birth time, ie btime)
   */
  Timestamp getCreationTime();

  /**
   * Set an attribute used to define the data structure
   *
   * @param key   the key
   * @param value the value
   * @return the data path for chaining
   */
  DataPath addAttribute(KeyNormalizer key, Object value);


  DataPath addAttribute(AttributeEnum key, Object value);

  /**
   * Add a variable.
   * It makes it possible to create a variable with a {@link Attribute#setValueProvider(Supplier)}
   */
  DataPath addAttribute(Attribute attribute);

  /**
   * A utility function that returns the records of a data path
   *
   * @return the records of a data path in a list of list
   */
  List<List<?>> getRecords();

  /**
   * Execute a {@link #isRuntime() runtime} and returns its results
   *
   * @return the result
   */
  DataPath execute();

  /**
   * @return the tabular type (ie with {@link TabularType#COMMAND} or {@link TabularType#DATA without exit code} )
   */
  TabularType getTabularType();

  /**
   * Set the tabular type
   */
  DataPath setTabularType(TabularType tabularType);


}
