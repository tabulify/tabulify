package net.bytle.db.model;

import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPathAbs;
import net.bytle.type.MapCaseIndependent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by gerard on 01-02-2016.
 * A class that contains the data structure definition
 * <p>
 * A table can be:
 * * "SQL TABLE",
 * * "SQL VIEW"
 * * "CSV"
 *
 * <p>
 * A table definition may be created:
 * * manually
 * * or through the metadata of the driver of a result set (in a sql database)
 * * or through a data def file
 * * or manually via code
 */

public abstract class DataDefAbs implements RelationDef {

  public static final String DATA_DEF_SUFFIX = "--datadef.yml";

  protected final DataPathAbs dataPath;

  private Map<String, ColumnDef> columnDefByName = new HashMap<>();

  private PrimaryKeyDef primaryKeyDef;

  /**
   * Table Property that can be used by other type of relation
   */
  private MapCaseIndependent<Object> properties = new MapCaseIndependent<>();

  /**
   * The identity string is for now the name of the foreign key
   * TODO ? but it would be better to implement on the column names
   * because not all foreign keys have a name (for instance Sqlite)
   */
  private HashMap<String, ForeignKeyDef> foreignKeys = new HashMap<>();


  private Set<UniqueKeyDef> uniqueKeys = new HashSet<>();

  public DataDefAbs(DataPathAbs dataPath) {
    this.dataPath = dataPath;
  }


  public static TableDef of(DataPathAbs dataPath) {
    return new TableDef(dataPath);
  }


  @Override
  public PrimaryKeyDef getPrimaryKey() {
    return primaryKeyDef;
  }


  @Override
  public List<ForeignKeyDef> getForeignKeys() {

    return new ArrayList<>(foreignKeys.values());

  }


  /**
   * Return the foreign Key for this primary key and this set of columns
   * If the key is already defined
   *
   * @param columnNames   the columns on the table
   * @param primaryKeyDef the foreign primary key
   * @return the foreignKeyDef
   */
  @Override
  public ForeignKeyDef foreignKeyOf(PrimaryKeyDef primaryKeyDef, String... columnNames) {

    assert primaryKeyDef != null : "To add a foreign key to (" + this.getDataPath().toString() + "), the primary key should not be null";
    assert columnNames.length > 0;

    // if the foreign key exist already, return it
    for (ForeignKeyDef foreignKeyDef : getForeignKeys()) {
      final PrimaryKeyDef foreignPrimaryKey = foreignKeyDef.getForeignPrimaryKey();
      if (foreignPrimaryKey != null) {
        if (foreignPrimaryKey.equals(primaryKeyDef)) {
          final List<String> childColumns = foreignKeyDef.getChildColumns().stream()
            .map(s -> s.getColumnName())
            .collect(Collectors.toList());
          if (childColumns.equals(Arrays.asList(columnNames))) {
            return foreignKeyDef;
          }
        }
      }
    }

    final String fkName = dataPath.getName() + "_fk" + foreignKeys.size();
    List<ColumnDef> columnDefs = Arrays.asList(columnNames).stream()
      .map(this::getColumnDef)
      .collect(Collectors.toList());

    ForeignKeyDef foreignKeyDef = ForeignKeyDef.of(primaryKeyDef, columnDefs)
      .setName(fkName);

    this.foreignKeys.put(fkName, foreignKeyDef);
    return foreignKeyDef;

  }


  @Override
  public List<UniqueKeyDef> getUniqueKeys() {
    return new ArrayList(uniqueKeys);
  }


  /**
   * Get and Create function
   *
   * @param columnName
   * @return
   */
  public UniqueKeyDef getOrCreateUniqueKey(String name, String... columnName) {

    UniqueKeyDef uniqueKeyDefToReturn = null;
    for (UniqueKeyDef uniqueKeyDef : uniqueKeys) {

      if (uniqueKeyDef.getColumns().equals(Arrays.asList(columnName))) {
        uniqueKeyDefToReturn = uniqueKeyDef;
      }

    }
    if (uniqueKeyDefToReturn == null) {
      uniqueKeyDefToReturn = UniqueKeyDef.of(this)
        .addColumns(
          Arrays.stream(columnName)
            .map(this::getColumnDef)
            .collect(Collectors.toList()))
        .name(name);
      uniqueKeys.add(uniqueKeyDefToReturn);
    }
    return uniqueKeyDefToReturn;


  }

  /**
   * Shortcut to add two columns as primary key
   *
   * @param columnNames - The column name of the primary key
   * @return - the table Def for a chaining initialization
   */
  @Override
  public DataDefAbs setPrimaryKey(String... columnNames) {

    this.primaryKeyOf(columnNames);
    return this;

  }


  /**
   * Add a unique key
   *
   * @param columnNames
   * @return the table def for chaining initialization
   */
  @Override
  public RelationDef addUniqueKey(String name, String... columnNames) {
    getOrCreateUniqueKey(name, columnNames);
    return this;
  }


  /**
   * Add a foreign key
   *
   * @param primaryKeyDef
   * @param columnNames
   * @return the table for initialization chaining
   */
  @Override
  public DataDefAbs addForeignKey(PrimaryKeyDef primaryKeyDef, String... columnNames) {
    try {
      foreignKeyOf(primaryKeyDef, columnNames);
    } catch (Exception e) {
      throw new RuntimeException("A problem occurs when trying to add a foreign to the table (" + this + ") towards the table (" + primaryKeyDef.getDataDef().getDataPath() + "). See the message below.", e);
    }
    return this;
  }

  /**
   * Add a foreign key
   *
   * @param primaryKeyDef
   * @param columnNames
   * @return the table for initialization chaining
   */
  @Override
  public DataDefAbs addForeignKey(PrimaryKeyDef primaryKeyDef, List<String> columnNames) {
    try {
      foreignKeyOf(primaryKeyDef, columnNames.toArray(new String[0]));
    } catch (Exception e) {
      throw new RuntimeException("A problem occurs when trying to add a foreign to the table (" + this + ") towards the table (" + primaryKeyDef.getDataDef().getDataPath() + "). See the message below.", e);
    }
    return this;
  }

  /**
   * @param dataPath    - the foreign primary table
   * @param columnNames - the column names of this tables
   * @return the tableDef for chaining initialization
   */
  @Override
  public DataDefAbs addForeignKey(DataPath dataPath, String... columnNames) {
    final PrimaryKeyDef primaryKey = dataPath.getDataDef().getPrimaryKey();
    if (primaryKey == null) {
      throw new RuntimeException("The data unit (" + dataPath + ") can't be added as foreign table for the table (" + this.getDataPath() + ") and its columns (" + String.join(",", columnNames) + ") because it has no primary key defined.");
    }
    this.foreignKeyOf(primaryKey, columnNames);
    return this;
  }


  @Override
  public DataDefAbs setPrimaryKey(List<String> columnNames) {
    primaryKeyOf(columnNames.toArray(new String[0]));
    return this;
  }


  @Override
  public void deleteForeignKey(ForeignKeyDef foreignKeyDef) {

    foreignKeyDef = foreignKeys.remove(foreignKeyDef.getName());
    if (foreignKeyDef == null) {

      throw new RuntimeException("The foreign key (" + foreignKeyDef.getName() + ") does not belong to the table (" + this + ") and could not be removed");
    }

  }


  /**
   * Property value are generally given via a {@link DataDefs data definition file}
   *
   * @param key
   * @return the property value of this table def
   */
  @Override
  public Object getProperty(String key) {
    return properties.get(key);
  }

  /**
   * Add a property for this table def
   *
   * @param key
   * @param value
   * @return the tableDef for initialization chaining
   */
  @Override
  public DataDefAbs addProperty(String key, Object value) {
    properties.put(key, value);
    return this;
  }

  /**
   * Property value are generally given via a {@link DataDefs data definition file}
   *
   * @return the properties value of this table def
   */
  @Override
  public MapCaseIndependent<Object> getProperties() {
    return properties;
  }


  @Override
  public ForeignKeyDef foreignKeyOf(PrimaryKeyDef primaryKey, List<String> columns) {
    return foreignKeyOf(primaryKey, columns.toArray(new String[0]));
  }

  @Override
  public PrimaryKeyDef primaryKeyOf(String... columnNames) {
    assert columnNames.length > 0 : "The number of columns should be greater than one for a primary key";

    this.primaryKeyDef = PrimaryKeyDef.of(this, columnNames);
    return this.primaryKeyDef;
  }


  @Override
  public DataPath getDataPath() {
    return dataPath;
  }


  @Override
  public String toString() {
    return "DataDef of " + dataPath;
  }


  /**
   * Shortcut alias function to get a property value of a key as long
   * @param key - the string
   * @return a property as a long
   */
  protected Long getPropertyAsLong(String key) {
    return this.properties.getAsLong(key);
  }

  @Override
  public DataDefAbs copy(DataPath sourceDataPath) {
    DataDefs.copy(sourceDataPath.getDataDef(),this);
    return this;
  }


}
