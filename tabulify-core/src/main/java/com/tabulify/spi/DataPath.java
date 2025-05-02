package com.tabulify.spi;

import com.tabulify.connection.Connection;
import com.tabulify.engine.StreamDependencies;
import com.tabulify.model.RelationDef;
import com.tabulify.sample.BytleSchema;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferProperties;
import com.tabulify.uri.DataUri;
import net.bytle.dag.Dependency;
import net.bytle.exception.NoParentException;
import net.bytle.exception.NoVariableException;
import net.bytle.exception.NotFoundException;
import com.tabulify.conf.Attribute;
import com.tabulify.conf.AttributeEnum;
import net.bytle.type.MediaType;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
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
public interface DataPath extends Comparable<DataPath>, Dependency, StreamDependencies {

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
   */
  String getRelativePath();

  /**
   * @return the id
   * It should be equivalent to {@link #toDataUri()})
   * <p>
   * Identifier is also used in the {@link Dependency#getId() dependency}
   * in order to determine object equality
   */
  String getId();

  /**
   * @return a {@link DataDef} object created from the data store metadata or an empty object if it does not exist
   * <p>
   * The function {@link #createRelationDef()} create only an empty object
   */
  RelationDef getOrCreateRelationDef();


  /**
   * A utility function that allows
   * some {@link #getScript()} script
   * processing that is only for query
   * For instance, a SQL query does not allow a `;` at the end
   *
   * @return the query
   */
  String getQuery();

  /**
   * @return the script
   */
  String getScript();

  /**
   * @param name - the sibling name
   * @return a sibling (ie on the path `/a/c`, the sibling `b` would be `/a/b`
   * <p>
   * Example with a data path equivalent to /foo/bar and foo as name, we get a DataPath of /foo/foo
   * Equivalent to the {@link java.nio.file.Path#resolveSibling(String)}
   */
  DataPath getSibling(String name);

  /**
   * @param name - a child name
   * @return a child (ie on the path `/a/c`, the child `b` would be `/a/c/b`
   * <p>
   * This is the equivalent to the {@link Path#resolve(String)}
   * but where:
   * * you can't use ".." and "."
   * * you can use only one argument
   */
  @SuppressWarnings("GrazieInspection")
  DataPath getChild(String name);

  /**
   * This is the equivalent to the {@link java.nio.file.Path#resolve(String)} (String)}
   * <p>
   * The function will return a data path from a {@link ResourcePath}
   *
   * @param stringPath the string path
   * @return the path
   */
  DataPath resolve(String stringPath);

  /**
   * @return the dependencies
   */
  Set<? extends Dependency> getDependencies();

  /**
   * @param description the description of the data path
   * @return the path
   */
  DataPath setDescription(String description);

  /**
   * @return the description of the data path
   */
  String getDescription();


  /**
   * @param name - the name of the path
   * @return a data path with a tabular structure
   * For instance:
   * * when you are in a file data system, you will get a csv
   * * when you are in a relational data system, you will get a table
   */
  DataPath getChildAsTabular(String name);

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
   * When processing a stream, another processing should have run before
   * This function returns the data path that would generate a dependency stream
   * <p>
   * This is the case in TPC-DS where the `returns` are generated from the `sales`
   *
   * @return the path that depends on this path for a select
   */
  DataPath getSelectStreamDependency() throws NotFoundException;

  /**
   * @return an empty {@link RelationDef} object
   * <p>
   * Use it when you don't want to get the  {@link RelationDef} from the underlying {@link DataSystem system}
   * <p>
   * If you use the {@link #getOrCreateRelationDef()}, the underlying metadata
   * from the system will be used
   * <p>
   * {@link BytleSchema} is a good example of usage
   */
  RelationDef createRelationDef();

  /**
   * @return a data def or null if it does not exist
   */
  RelationDef getRelationDef();

  /**
   * @return the data uri
   */
  DataUri toDataUri();

  /**
   * @return the absolute path string from the data system (not the connection)
   * ie the full qualified path for a file system
   */
  String getAbsolutePath();


  byte[] getByteDigest(String digestAlgo) throws NoSuchAlgorithmException, IOException;


  /**
   * @param name the attribute name
   * @return the variable
   * @throws NoVariableException - when the variable was not found
   *                             Why string and not {@link AttributeEnum} because variable may be created dynamically
   *                             (for instance backref of regexp), and therefore may be not known in advance
   */
  Attribute getAttribute(String name) throws NoVariableException;

  /**
   * @param attribute - the attribute
   * @return the variable
   * @throws NoVariableException - when the variable was not found
   */
  Attribute getAttribute(AttributeEnum attribute) throws NoVariableException;

  /**
   * Set an attribute used to define the data structure
   *
   * @param key   the key
   * @param value the value
   * @return the data path for chaining
   */
  DataPath addAttribute(String key, Object value);

  DataPath addAttribute(AttributeEnum key, Object value);

  Set<Attribute> getAttributes();

  DataPath mergeDataPathAttributesFrom(DataPath source);

  DataPath mergeDataDefinitionFromYamlFile(Path dataDefPath);

  DataPath mergeDataDefinitionFromYamlMap(Map<String, ?> document);

  DataPath setDataAttributes(Map<String, ?> dataAttributes);

  /**
   * Meta = {@link #getAttributes() DataPath Properties}  and {@link #getOrCreateRelationDef() Structure}
   *
   * @param mergeFrom the source path
   * @return the path
   */
  DataPath mergeDataDefinitionFrom(DataPath mergeFrom);


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
   * @return true if the data path is a script
   */
  boolean isScript();

  /**
   * @return the attributes / properties in a data path format
   */
  DataPath toAttributesDataPath();

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
  InsertStream getInsertStream(DataPath source, TransferProperties transferProperties);

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


  InsertStream getInsertStream(TransferProperties transferProperties);

  DataPath getParent() throws NoParentException;


  /**
   * @return true if the file headers are stored alongside the content
   * It's important for transfer operation.
   * You can use block operation to append a source file to an existing file if it has no header.
   */
  boolean hasHeaderInContent();

  /**
   * If this data path has a script or null
   */
  DataPath getScriptDataPath();

  Attribute getAttributeSafe(AttributeEnum sqlDataPathAttribute);

  /**
   * Add a variable.
   * It makes it possible to create a variable with a {@link Attribute#setValueProvider(Supplier)}
   */
  DataPath addAttribute(Attribute attribute);

}
