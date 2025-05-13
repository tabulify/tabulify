package com.tabulify.model;

import com.tabulify.DbLoggers;
import com.tabulify.diff.DataPathDataComparison;
import com.tabulify.memory.MemoryDataPath;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;
import com.tabulify.stream.InsertStream;
import net.bytle.exception.NoColumnException;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.Strings;

import java.util.*;
import java.util.stream.Collectors;

import static com.tabulify.model.ColumnAttribute.*;


public abstract class RelationDefAbs implements RelationDef {


  private final DataPath dataPath;
  private PrimaryKeyDef primaryKeyDef;

  private final Set<ForeignKeyDef> foreignKeys = new HashSet<>();


  private Set<UniqueKeyDef> uniqueKeys = new HashSet<>();

  public <T extends DataPath> RelationDefAbs(T dataPath) {
    this.dataPath = dataPath;
  }

  @Override
  public PrimaryKeyDef getPrimaryKey() {
    return primaryKeyDef;
  }


  @Override
  public List<ForeignKeyDef> getForeignKeys() {

    return new ArrayList<>(foreignKeys);

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

    assert primaryKeyDef != null : "To add a foreign key to (" + this.getDataPath().toString() + ") on the columns (" + Arrays.toString(columnNames) + "), the primary key should not be null";
    assert columnNames.length > 0;

    // if the foreign key exist already, return it
    for (ForeignKeyDef foreignKeyDef : getForeignKeys()) {
      final PrimaryKeyDef foreignPrimaryKey = foreignKeyDef.getForeignPrimaryKey();
      if (foreignPrimaryKey != null) {
        if (foreignPrimaryKey.equals(primaryKeyDef)) {
          final List<String> childColumns = foreignKeyDef.getChildColumns().stream()
            .map(ColumnDef::getColumnName)
            .collect(Collectors.toList());
          if (childColumns.equals(Arrays.asList(columnNames))) {
            return foreignKeyDef;
          }
        }
      }
    }


    ForeignKeyDef foreignKeyDef = ForeignKeyDef.createOf(this, primaryKeyDef, columnNames);

    this.foreignKeys.add(foreignKeyDef);
    return foreignKeyDef;

  }


  @Override
  public List<UniqueKeyDef> getUniqueKeys() {
    return new ArrayList<>(uniqueKeys);
  }


  /**
   * Get and Create function
   * The name is not mandatory to create a primary key
   *
   * @param columnName the column name that form the unique key
   * @return the unique key
   */
  public UniqueKeyDef getOrCreateUniqueKey(String... columnName) {

    UniqueKeyDef uniqueKeyDefToReturn = null;
    for (UniqueKeyDef uniqueKeyDef : uniqueKeys) {

      List<String> uniqueKeyColumns = uniqueKeyDef.getColumns().stream().map(ColumnDef::getColumnName).collect(Collectors.toList());
      if (uniqueKeyColumns.equals(Arrays.asList(columnName))) {
        uniqueKeyDefToReturn = uniqueKeyDef;
      }

    }
    if (uniqueKeyDefToReturn == null) {
      try {
        List<ColumnDef> list = new ArrayList<>();
        for (String s : columnName) {
          ColumnDef columnDef = getColumnDef(s);
          list.add(columnDef);
        }
        uniqueKeyDefToReturn = UniqueKeyDef.of(this)
          .addColumns(list);
        uniqueKeys.add(uniqueKeyDefToReturn);
      } catch (NoColumnException e) {
        String message = "A unique key of the resource (" + this.getDataPath() + ") was not created  because a column was not found in the metadata." + e.getMessage();
        if (this.getDataPath().getConnection().getTabular().isIdeEnv()) {
          throw new RuntimeException(message, e);
        } else {
          DbLoggers.LOGGER_DB_ENGINE.severe(message);
        }
      }
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
  public RelationDefAbs setPrimaryKey(String... columnNames) {

    this.primaryKeyOf(columnNames);
    return this;

  }

  /**
   * Add a unique key
   *
   * @param columnNames the unique column names
   * @return the table def for chaining initialization
   */
  @Override
  public RelationDefAbs addUniqueKey(String... columnNames) {
    getOrCreateUniqueKey(columnNames);
    return this;
  }

  /**
   * Add a foreign key
   *
   * @param primaryKeyDef the external primary key
   * @param columnNames   the internal column names
   * @return the table for initialization chaining
   */
  @Override
  public RelationDefAbs addForeignKey(PrimaryKeyDef primaryKeyDef, String... columnNames) {
    try {
      foreignKeyOf(primaryKeyDef, columnNames);
    } catch (Exception e) {
      throw new RuntimeException("A problem occurs when trying to add a foreign to the table (" + this + ") towards the table (" + primaryKeyDef.getRelationDef().getDataPath() + "). See the message below.", e);
    }
    return this;
  }

  /**
   * Add a foreign key
   *
   * @param primaryKeyDef - the external primary key
   * @param columnNames   - the internal column names
   * @return the table for initialization chaining
   */
  @Override
  public RelationDefAbs addForeignKey(PrimaryKeyDef primaryKeyDef, List<String> columnNames) {
    try {
      foreignKeyOf(primaryKeyDef, columnNames.toArray(new String[0]));
    } catch (Exception e) {
      throw new RuntimeException(
        Strings.createMultiLineFromStrings("A problem occurs when trying to add a foreign to the table (" + this + ") towards the table (" + primaryKeyDef.getRelationDef().getDataPath() + ").",
          e.getMessage()).toString()
        , e);
    }
    return this;
  }

  /**
   * @param dataPath    - the foreign primary table
   * @param columnNames - the column names of this tables
   * @return the tableDef for chaining initialization
   */
  @Override
  public RelationDefAbs addForeignKey(DataPath dataPath, String... columnNames) {
    assert this.getDataPath().getConnection().equals(dataPath.getConnection()) : "The foreign data path (" + dataPath + ") has a data store (" + dataPath.getConnection() + ") that is not the same (" + this.getDataPath().getConnection() + ") than the data path (" + this.getDataPath() + ")";
    final PrimaryKeyDef primaryKey = dataPath.getOrCreateRelationDef().getPrimaryKey();
    if (primaryKey == null) {
      throw new RuntimeException("The data unit (" + dataPath + ") can't be added as a foreign table for the table (" + this.getDataPath() + ") and its columns (" + String.join(",", columnNames) + ") because it has no primary key defined.");
    }
    this.foreignKeyOf(primaryKey, columnNames);
    return this;
  }


  @Override
  public RelationDefAbs setPrimaryKey(List<String> columnNames) {
    primaryKeyOf(columnNames.toArray(new String[0]));
    return this;
  }


  @Override
  public void deleteForeignKey(ForeignKeyDef foreignKeyDef) {

    boolean b = foreignKeys.remove(foreignKeyDef);
    if (!b) {

      throw new RuntimeException("The foreign key (" + foreignKeyDef.getName() + ") does not belong to the table (" + this + ") and could not be removed");
    }

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
  public String toString() {
    return "Relational Def of " + this.getDataPath();
  }

  /**
   * An utility function that creates a column
   * by looking the sqlDataType via it's class
   *
   * @param columnName the column name
   * @param clazz      the clazz
   * @return the object for chaining
   */
  public ColumnDef getOrCreateColumn(String columnName, Class<?> clazz) {
    SqlDataType sqlDataType = this.getDataPath().getConnection().getSqlDataType(clazz);
    return this.getOrCreateColumn(columnName, sqlDataType, clazz);
  }

  public ColumnDef getOrCreateColumn(String columnName) {
    SqlDataType sqlDataType = this.getDataPath().getConnection().getSqlDataType(String.class);
    return this.getOrCreateColumn(columnName, sqlDataType, String.class);
  }

  public ColumnDef getOrCreateColumn(String columnName, Integer typeCode) {
    SqlDataType sqlDataType = this.getDataPath().getConnection().getSqlDataType(typeCode);
    return this.getOrCreateColumn(columnName, sqlDataType, sqlDataType.getSqlClass());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RelationDefAbs that = (RelationDefAbs) o;
    return this.getDataPath().equals(that.getDataPath());
  }


  @Override
  public int hashCode() {
    return Objects.hash(this.getDataPath());
  }

  /**
   * @param targetDataPath the target to compare to
   * @return the DataPathDataComparison
   * Compare only the data structure (not the constraints)
   */
  @Override
  public DataPathDataComparison compareData(DataPath targetDataPath) {

    DataPath left = this.toColumnsDataPathBy(NAME);
    DataPath right = targetDataPath.getOrCreateRelationDef().toColumnsDataPathBy(NAME);

    return DataPathDataComparison.create(left, right).compareData();

  }

  /**
   * The columns without any constraints
   * (Used to make a structure difference)
   * The data is ordered by name
   *
   * @param orderColumn -  order of the data set (Generally name or position)
   * @return the path
   */
  @Override
  public DataPath toColumnsDataPathBy(ColumnAttribute orderColumn, ColumnAttribute... columnAttributes) {
    if (columnAttributes.length == 0) {
      /**
       * These are the attributes that can be used to compare
       * a query metadata
       */
      columnAttributes = new ColumnAttribute[]{
        POSITION, NAME, TYPE, PRECISION, SCALE
      };
    }
    MemoryDataPath structureComparisonDataPath = this.getDataPath().getConnection().getTabular().getMemoryDataStore().getDataPath("structure" + this.getDataPath().getLogicalName());
    RelationDef relationDef = structureComparisonDataPath.getOrCreateRelationDef();
    for (ColumnAttribute columnAttribute : columnAttributes) {
      relationDef.addColumn(KeyNormalizer.createSafe(columnAttribute).toSqlCaseSafe(), columnAttribute.getValueClazz());
    }

    try (
      InsertStream insertStream = structureComparisonDataPath.getInsertStream()
    ) {

      List<ColumnDef> columnDefs = this.getColumnDefs()
        .stream()
        .sorted((s1, s2) -> {
            switch (orderColumn) {
              case POSITION:
                return s1.getColumnPosition().compareTo(s2.getColumnPosition());
              case TYPE:
                return s1.getDataType().getSqlName().compareTo(s2.getDataType().getSqlName());
              default:
                /**
                 * By name default
                 */
                return s1.getColumnName().compareTo(s2.getColumnName());
            }
          }
        )
        .collect(Collectors.toList());

      for (ColumnDef columnDef : columnDefs) {

        Object[] columnsColumns = new Object[columnAttributes.length];

        for (int i = 0; i < columnAttributes.length; i++) {
          switch (columnAttributes[i]) {
            case POSITION:
              columnsColumns[i] = columnDef.getColumnPosition();
              break;
            case NAME:
              columnsColumns[i] = columnDef.getColumnName();
              break;
            case TYPE:
              columnsColumns[i] = columnDef.getDataType().getSqlName();
              break;
            case NULLABLE:
              columnsColumns[i] = columnDef.isNullable();
              break;
            case PRECISION:
              columnsColumns[i] = columnDef.getPrecision();
              break;
            case SCALE:
              columnsColumns[i] = columnDef.getScale();
              break;
            case AUTOINCREMENT:
              columnsColumns[i] = columnDef.isAutoincrement();
              break;
            case GENERATED:
              columnsColumns[i] = columnDef.isGeneratedColumn();
              break;
          }
        }

        insertStream.insert(columnsColumns);
      }

    }
    return structureComparisonDataPath;
  }

  /**
   * Remove the metadata primary key
   */
  @Override
  public RelationDef removeMetadataPrimaryKey() {
    for (ColumnDef column : this.primaryKeyDef.getColumns()) {
      column.setNullable(true);
    }
    this.primaryKeyDef = null;
    return this;
  }

  /**
   * Remove unique key
   *
   * @param uniqueKeyDef the unique key
   */
  @Override
  public RelationDef removeMetadataUniqueKey(UniqueKeyDef uniqueKeyDef) {

    this.uniqueKeys.remove(uniqueKeyDef);
    return this;

  }

  @Override
  public RelationDef removeAllMetadataUniqueKeys() {

    this.uniqueKeys = new HashSet<>();
    return this;

  }


  @Override
  public DataPath getDataPath() {
    return this.dataPath;
  }


  @Override
  public RelationDef mergeStructWithoutConstraints(DataPath fromDataPath) {
    assert fromDataPath != null : "The target data definition cannot be null";
    RelationDef fromDataDef = fromDataPath.getOrCreateRelationDef();

    int columnCount = fromDataDef.getColumnsSize();
    for (int i = 1; i <= columnCount; i++) {
      ColumnDef columnDef = fromDataDef.getColumnDef(i);
      ColumnDef targetColumn;
      try {
        targetColumn = this.getColumnDef(columnDef.getColumnName());
      } catch (NoColumnException e) {
        targetColumn = this.getOrCreateColumn(columnDef.getColumnName(), columnDef.getDataType(), columnDef.getClazz());
      }
      targetColumn
        .precision(columnDef.getPrecision())
        .scale(columnDef.getScale())
        .setComment(columnDef.getComment())
        .setAllVariablesFrom(columnDef);
    }

    return this;
  }

  /**
   * Copy the struct (struct = columns + local constraint (fk and uk))
   * <p>
   * If the target have any columns of the source with a different class (data type),
   * you will get an error. To avoid it, you may want to use the {@link #mergeStructWithoutConstraints(DataPath)}
   * function instead
   */
  @Override
  public RelationDefAbs copyStruct(DataPath from) {
    assert from != null : "The source data definition cannot be null";

    // Add the columns
    int columnCount = from.getOrCreateRelationDef().getColumnsSize();
    for (int i = 1; i <= columnCount; i++) {
      ColumnDef columnDef = from.getRelationDef().getColumnDef(i);
      SqlDataType targetDataType = this.getDataPath().getConnection().getSqlDataTypeFromSourceDataType(columnDef.getDataType());
      this.getOrCreateColumn(columnDef.getColumnName(), targetDataType, targetDataType.getSqlClass())
        .precision(columnDef.getPrecision())
        .scale(columnDef.getScale())
        .setNullable(columnDef.isNullable())
        .setComment(columnDef.getComment())
        .setAllVariablesFrom(columnDef);
    }


    // Add the primary key
    copyPrimaryKeyFrom(from);

    copyUniqueKeysFrom(from);

    return this;
  }

  @Override
  public RelationDef copyPrimaryKeyFrom(DataPath from) {
    final PrimaryKeyDef sourcePrimaryKey = from.getOrCreateRelationDef().getPrimaryKey();
    if (sourcePrimaryKey != null) {
      final List<String> columns = sourcePrimaryKey.getColumns().stream()
        .map(ColumnDef::getColumnName)
        .collect(Collectors.toList());
      this.setPrimaryKey(columns);
    }
    return this;
  }

  @Override
  public <D1 extends DataPath, D2 extends DataPath, D3 extends DataPath> RelationDef mergeDataDef(D1 targetDataPath, Map<D2, D3> targetSources) {

    /**
     * The structure
     */
    mergeStruct(targetDataPath);

    /**
     * Add the foreign keys
     */
    copyForeignKeysFrom(targetDataPath, targetSources);

    return this;

  }

  /**
   * The signature is the data path because this is the most used level
   * and not a data def
   *
   * @param fromDataPath the source data path
   * @return the object for chaining
   */
  @Override
  public RelationDef mergeDataDef(DataPath fromDataPath) {

    return mergeDataDef(fromDataPath, null);

  }

  @Override
  public <D1 extends DataPath, D2 extends DataPath, D3 extends DataPath> RelationDef copyForeignKeysFrom(D1 targetDataPath, Map<D2, D3> targetSource) {
    if (targetSource == null) {
      targetSource = new HashMap<>();
    }
    final List<ForeignKeyDef> targetForeignKeyDefs = targetDataPath.getRelationDef().getForeignKeys();
    for (ForeignKeyDef targetForeignKeyDef : targetForeignKeyDefs) {
      DataPath targetForeignDataPath = targetForeignKeyDef.getForeignPrimaryKey().getRelationDef().getDataPath();

      DataPath sourceForeignDataPath = null;
      DataPath target = this.getDataPath();
      if (!targetSource.isEmpty()) {
        // In case of cross system metadata copy, the name of the foreign key table
        // may be not the same as the source table
        // example: d_cat and D_CAT, it happens when quote are disabled on oracle as Oracle will put all non-quoted name in UPPERCASE
        //noinspection SuspiciousMethodCalls
        sourceForeignDataPath = targetSource.get(targetForeignDataPath);
      }
      if (sourceForeignDataPath == null) {
        String targetName = targetForeignDataPath.getName();
        sourceForeignDataPath = target.getSibling(targetName);
      }

      // Does the table exist in the target
      if (Tabulars.exists(sourceForeignDataPath)) {
        PrimaryKeyDef targetPrimaryKey = sourceForeignDataPath.getOrCreateRelationDef().getPrimaryKey();
        assert targetPrimaryKey != null : "Foreign Key not copied from (" + targetDataPath + ") to (" + target + "): The foreign data path (" + sourceForeignDataPath + ") exists but does not have any primary key. There is a inconsistency bug somewhere.";
        List<String> targetForeignPrimaryKeyColumns = targetPrimaryKey.
          getColumns().stream()
          .map(ColumnDef::getColumnName)
          .collect(Collectors.toList());
        List<String> sourceForeignPrimaryKeyColumns = targetForeignDataPath.getRelationDef().getPrimaryKey().getColumns().stream().map(ColumnDef::getColumnName).collect(Collectors.toList());
        // Do they have the same primary key columns
        if (targetForeignPrimaryKeyColumns.equals(sourceForeignPrimaryKeyColumns)) {
          // Create it then
          target.getRelationDef().addForeignKey(sourceForeignDataPath,
            targetForeignKeyDef.getChildColumns().stream()
              .map(ColumnDef::getColumnName)
              .toArray(String[]::new)
          );
        } else {
          DbLoggers.LOGGER_DB_ENGINE.warning("Foreign Key not copied: The primary columns of the source (" + sourceForeignPrimaryKeyColumns + ") are not the same than the target (" + targetForeignPrimaryKeyColumns);
        }
      } else {
        DbLoggers.LOGGER_DB_ENGINE.warning("Foreign Key not copied: The target data path (" + sourceForeignDataPath + ") does not exist");
      }
    }
    return this;
  }

  @Override
  public RelationDef copyForeignKeysFrom(DataPath fromDataPath) {
    return copyForeignKeysFrom(fromDataPath, null);
  }

  /**
   * The struct = columns + primary key + unique key
   *
   * @param fromDataPath the source
   * @return the relation
   */
  @Override
  public RelationDef mergeStruct(DataPath fromDataPath) {

    /**
     * Merge without constraints
     */
    mergeStructWithoutConstraints(fromDataPath);

    /**
     * Add the constraints
     */
    // Add the primary key
    if (this.getPrimaryKey() == null) {
      copyPrimaryKeyFrom(fromDataPath);
    }

    // Add the unique keys
    copyUniqueKeysFrom(fromDataPath);

    // Add null constraint
    for (int i = 1; i <= fromDataPath.getRelationDef().getColumnsSize(); i++) {
      this.getColumnDef(i).setNullable(fromDataPath.getRelationDef().getColumnDef(i).isNullable());
    }
    PrimaryKeyDef primaryKeyDef = this.getPrimaryKey();
    if (primaryKeyDef != null) {
      for (ColumnDef column : primaryKeyDef.getColumns()) {
        column.setNullable(true);
      }
    }

    return this;

  }

  private RelationDef copyUniqueKeysFrom(DataPath fromDataPath) {
    for (UniqueKeyDef uniqueKeyDef : fromDataPath.getRelationDef().getUniqueKeys()) {
      final String[] columns = uniqueKeyDef.getColumns().stream()
        .map(ColumnDef::getColumnName)
        .toArray(String[]::new);
      this.addUniqueKey(columns);
    }
    return this;
  }

  @Override
  public RelationDef copyDataDef(DataPath fromDataPath) {
    copyStruct(fromDataPath);
    copyForeignKeysFrom(fromDataPath);
    return this;
  }

}
