package net.bytle.db.spi;

import net.bytle.db.database.DataStore;
import net.bytle.db.engine.Relational;
import net.bytle.db.model.RelationDef;
import net.bytle.db.uri.DataUri;

import java.util.List;

public interface DataPath extends Comparable<DataPath>, Relational {

  DataStore getDataStore();

  /**
   * A name is part of the path (if this is not the case, you can set a {@link #setDescription(String)}
   *
   * @return
   */
  String getName();

  List<String> getNames();

  String getPath();

  String getId();

  /**
   * @return a {@link RelationDef} object created from the data store metadata or an empty object if it does not exist
   *
   * The function {@link #createDataDef()} create only an empty object
   */
  RelationDef getOrCreateDataDef();

  DataPath setQuery(String query);

  String getQuery();

  DataUri getDataUri();

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
   * This is the equivalent to the {@link #resolve(String...)}
   * but where:
   * * you can't use .. and .
   * * you can use only one argument
   */
  DataPath getChild(String name);

  /**
   * This is the equivalent to the {@link java.nio.file.Path#resolve(String)} (String)}
   * * but where:
   *
   * @param names
   * @return
   */
  DataPath resolve(String... names);

  List<DataPath> getForeignKeyDependencies();

  DataPath setDescription(String description);

  String getDescription();

  DataPath getChild(String name, RelationDef datadef);

  /**
   *
   * @param name
   * @return a data path with a tabular structure
   * For instance:
   *   * when you are in a file data system, you will get a csv
   *   * when you are in a relational data system, you will get a table
   */
  DataPath getChildAsTabular(String name);

  /**
   * The type is the structure of the data path that offerst the data store. ie
   *   * for a file system:
   *      * csv
   *      * json
   *   * for a memory system:
   *      * list
   *      * queue
   *      * gen
   *   * for a relational system
   *      * table
   *      * view
   *   ...
   * @return
   */
  String getType();

  DataPath getSelectStreamDependency();

  /**
   *
   * @return an empty {@link RelationDef} object
   * This function is used mostly to create a data def in memory
   * with test that will not clash with the actual data definition in the data store
   *
   * {@link net.bytle.db.sample.BytleSchema} is a good example of usage
   */
  RelationDef createDataDef();

}
