package net.bytle.db.spi;

import net.bytle.db.database.DataStore;
import net.bytle.db.engine.Relational;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.RelationDef;
import net.bytle.db.model.TableDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An object that may be used to locate a data container (such as a file or a table) in a data system (file system, relational database).
 * It will typically represent a system dependent data path.
 */
public abstract class DataPathAbs implements Comparable<DataPath>, Relational, DataPath {

  // The query is here because even if it defines a little bit the structure
  // (data def), for now, we get it after its execution
  // if you put the query on the data def you got a recursion
  private String query;

  protected RelationDef relationDef;
  private String description;


  @Override
  public String getId() {
    final String path = getPath();
    assert path != null : "Path cannot be null";
    final DataStore dataStore = getDataStore();
    final String databaseName = dataStore.getName();
    return path + "@" + databaseName;
  }

  public int compareTo(DataPath o) {
    return this.getId().compareTo(o.getId());
  }

  @Override
  public String toString() {
    return getId();
  }


  @Override
  public RelationDef getOrCreateDataDef() {
    return createDataDef();
  }

  @Override
  public RelationDef createDataDef() {
    if (this.relationDef == null) {
      this.relationDef = TableDef.of(this);
    }
    return relationDef;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DataPathAbs dataPath = (DataPathAbs) o;
    return getId().equals(dataPath.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId());
  }


  @Override
  public DataPath setQuery(String query) {
    this.query = query;
    return this;
  }

  /**
   * @return the query definition (select) or null if it's not a query
   */
  @Override
  public String getQuery() {
    return this.query;
  }


  /**
   * @return the parent (ie the foreign key relationship)
   */
  @Override
  public List<DataPath> getForeignKeyDependencies() {

    List<ForeignKeyDef> foreignKeys = this.getOrCreateDataDef() != null ? this.getOrCreateDataDef().getForeignKeys() : new ArrayList<>();
    List<DataPath> parentDataPaths = new ArrayList<>();
    if (foreignKeys.size() > 0) {

      for (ForeignKeyDef foreignKeyDef : foreignKeys) {
        parentDataPaths.add(foreignKeyDef.getForeignPrimaryKey().getDataDef().getDataPath());
      }

    }
    return parentDataPaths;

  }

  /**
   * @param description - set a description (to be able to label queries)
   * @return
   */
  @Override
  public DataPath setDescription(String description) {
    this.description = description;
    return this;
  }


  /**
   *
   * @return the description
   */
  @Override
  public String getDescription() {
    return  this.description;
  }


  @Override
  public DataPath getSelectStreamDependency() {
    return null;
  }

}
