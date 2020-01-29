package net.bytle.db.spi;

import net.bytle.db.database.DataStore;
import net.bytle.db.model.TableDef;
import net.bytle.db.uri.DataUri;

import java.util.List;
import java.util.Objects;

/**
 * An object that may be used to locate a data container (such as a file or a table) in a data system (file system, relational database).
 * It will typically represent a system dependent data path.
 */
public abstract class DataPath implements Comparable<DataPath> {

  // The query is here because even if it defines a little bit the structure
  // (data def), for now, we get it after its execution
  // if you put the query on the data def you got a recursion
  private String query;

  protected TableDef dataDef;


  public abstract TableSystem getDataSystem();


  public abstract String getName();

  public abstract List<String> getPathParts();

  private String getId() {
    final String path = getPath();
    assert path != null : "Path cannot be null";
    final DataStore dataStore = getDataSystem().getDataStore();
    final String databaseName = dataStore.getName();
    return path + "@" + databaseName;
  }

  public abstract String getPath();

  public int compareTo(DataPath o) {
    return this.getId().compareTo(o.getId());
  }

  @Override
  public String toString() {
    return getId();
  }


  public TableDef getDataDef() {
    if (this.dataDef == null) {
      this.dataDef = TableDef.of(this);
    }
    return dataDef;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DataPath dataPath = (DataPath) o;
    return getId().equals(dataPath.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId());
  }


  public DataPath setQuery(String query) {
    this.query = query;
    return this;
  }

  /**
   * @return the query definition (select) or null if it's not a query
   */
  public String getQuery() {
    return this.query;
  }

  public abstract DataUri getDataUri();

  /**
   *
   * @param name - the sibling name
   * @return a sibling (ie on the path `/a/c`, the sibling `b` would be `/a/b`
   *
   * Example with a data path equivalent to /foo/bar and foo as name, we get a DataPath of /foo/foo
   * Equivalent to the {@link java.nio.file.Path#resolveSibling(String)}
   */
  public abstract DataPath getSibling(String name);

  /**
   *
   * @param name - a child name
   * @return a child (ie on the path `/a/c`, the child `b` would be `/a/c/b`
   *
   * This is the equivalent to the {@link #resolve(String...)}
   * but where:
   *   * you can't use .. and .
   *   * you can use only one argument
   *
   */
  public abstract DataPath getChild(String name);


  /**
   * This is the equivalent to the {@link java.nio.file.Path#resolve(String)} (String)}
   *    * but where:
   * @param names
   * @return
   */
  public abstract DataPath resolve(String... names);

}
