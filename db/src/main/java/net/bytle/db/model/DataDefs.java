package net.bytle.db.model;


import net.bytle.db.Tabular;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;
import net.bytle.type.Arrayss;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DataDef(s)tatic functions
 * They are not meant to be used directly, use a {@link RelationDef object instead}
 *
 */
public class DataDefs {

  static final Logger logger = LoggerFactory.getLogger(DataDefs.class);

  /**
   * Merge the tables property {@link TableDef#getProperty(String)}
   * and the column property {@link ColumnDef#getProperty(String)} into one.
   * <p>
   * The first table has priority.
   * If a property does exist in the first and second table, the first will kept.
   *
   * @param firstTable
   * @param secondTable
   * @return the first table object updated
   */
  public static void mergeProperties(RelationDef firstTable, RelationDef secondTable) {

    Map<String, Object> firstTableProp = firstTable.getProperties();
    for (Map.Entry<String, Object> entry : secondTable.getProperties().entrySet()) {
      if (!firstTableProp.containsKey(entry.getKey())) {
        firstTableProp.put(entry.getKey(), entry.getValue());
      }
    }

    for (ColumnDef<?> columnDefFirstTable : firstTable.getColumnDefs()) {
      ColumnDef<?> columnSecondTable = secondTable.getColumnDef(columnDefFirstTable.getColumnName());
      if (columnSecondTable != null) {
        Map<String, Object> columnPropertiesFirstTable = columnDefFirstTable.getProperties();
        final Map<String, Object> properties = columnSecondTable.getProperties();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
          if (!columnPropertiesFirstTable.containsKey(entry.getKey())) {
            columnPropertiesFirstTable.put(entry.getKey(), entry.getValue());
          }
        }
      }
    }

  }


  /**
   * Add the columns to the targetDef from the sourceDef
   * <p>
   * If the target have any columns of the source with a different class (data type),
   * you will get an error. To avoid it, you may want to use the {@link #mergeColumns(RelationDef, RelationDef)}
   * function instead
   *
   * @param sourceDef
   * @param targetDef
   */
  public static void addColumns(RelationDef sourceDef, RelationDef targetDef) {

    assert sourceDef != null : "SourceDef should not be null";
    int columnCount = sourceDef.getColumnsSize();
    for (int i = 0; i < columnCount; i++) {
      ColumnDef columnDef = sourceDef.getColumnDef(i);
      targetDef.getOrCreateColumn(columnDef.getColumnName(), columnDef.getClazz())
        .typeCode(columnDef.getDataType().getTypeCode())
        .precision(columnDef.getPrecision())
        .scale(columnDef.getScale())
        .setNullable(columnDef.getNullable())
        .comment(columnDef.getComment())
        .addAllProperties(columnDef);
    }


  }

  public static DataPath getColumnsDataPath(Tabular tabular, List<DataPath> dataPaths) {

    DataPath columnsInfoDataPath = tabular.getDataPath("columns")
      .getOrCreateDataDef()
      .getDataPath();

    if (dataPaths.size() >= 1) {
      columnsInfoDataPath.getOrCreateDataDef()
        .addColumn("#")
        .addColumn("Table Name");
    }
    columnsInfoDataPath.getOrCreateDataDef()
      .addColumn("Position")
      .addColumn("Column Name")
      .addColumn("Data Type")
      .addColumn("Primary Key")
      .addColumn("Not Null")
      //.addColumn("Default")
      .addColumn("Auto Increment")
      .addColumn("Description");

    try (
      InsertStream insertStream = Tabulars.getInsertStream(columnsInfoDataPath);
    ) {
      int i = 0;
      for (DataPath dataPath : dataPaths) {
        for (ColumnDef columnDef : dataPath.getOrCreateDataDef().getColumnDefs()) {

          Object[] columnsColumns = {
            columnDef.getColumnPosition(),
            columnDef.getColumnName(),
            columnDef.getDataType().getTypeNames(),
            (dataPath.getOrCreateDataDef().getPrimaryKey().getColumns().contains(columnDef) ? "x" : ""),
            (columnDef.getNullable() ? "x" : ""),
            // columnDef.getDefault(),
            columnDef.getIsAutoincrement(),
            columnDef.getDescription()};

          if (dataPaths.size() >= 1) {
            i++;
            Object[] tablesColumns = {i, dataPath.getName()};
            columnsColumns = Arrayss.concat(tablesColumns, columnsColumns);
          }

          insertStream.insert(columnsColumns);
        }
      }
    }
    return columnsInfoDataPath;
  }



  /**
   * Add the foreign key from the source to the target
   * if the foreign tables exist in the target
   *
   * @param source
   * @param target
   */
  public static void addForeignKeys(RelationDef source, RelationDef target) {
    final List<ForeignKeyDef> foreignKeyDefs = source.getForeignKeys();
    for (ForeignKeyDef foreignKeyDef : foreignKeyDefs) {
      DataPath sourceForeignDataPath = foreignKeyDef.getForeignPrimaryKey().getDataDef().getDataPath();
      DataPath targetForeignDataPath = target.getDataPath().getSibling(sourceForeignDataPath.getName());
      // Does the table exist in the target
      if (Tabulars.exists(targetForeignDataPath)) {
        PrimaryKeyDef targetPrimaryKey = targetForeignDataPath.getOrCreateDataDef().getPrimaryKey();
        assert targetPrimaryKey!=null: "Foreign Key not copied: The foreign data path (" + targetForeignDataPath + ") exists but does not have any primary key. There is a inconsistency bug somewhere.";
        List<String> targetForeignPrimaryKeyColumns = targetPrimaryKey.
          getColumns().stream()
          .map(ColumnDef::getColumnName)
          .collect(Collectors.toList());
        List<String> sourceForeignPrimaryKeyColumns = sourceForeignDataPath.getOrCreateDataDef().getPrimaryKey().getColumns().stream().map(ColumnDef::getColumnName).collect(Collectors.toList());
        // Do they have the same primary key columns
        if (targetForeignPrimaryKeyColumns.equals(sourceForeignPrimaryKeyColumns)) {
          // Create it then
          target.addForeignKey(targetForeignDataPath,
            foreignKeyDef.getChildColumns().stream()
              .map(ColumnDef::getColumnName)
              .toArray(String[]::new)
          );
        } else {
          logger.warn("Foreign Key not copied: The primary columns of the source (" + sourceForeignPrimaryKeyColumns + ") are not the same than the target (" + targetForeignPrimaryKeyColumns);
        }
      } else {
        logger.warn("Foreign Key not copied: The target data path (" + targetForeignDataPath + ") does not exist");
      }
    }
  }

  public static void addPrimaryKey(RelationDef source, RelationDef target) {
    final PrimaryKeyDef sourcePrimaryKey = source.getPrimaryKey();
    if (sourcePrimaryKey != null) {
      final List<String> columns = sourcePrimaryKey.getColumns().stream()
        .map(s -> s.getColumnName())
        .collect(Collectors.toList());
      target.setPrimaryKey(columns);
    }
  }

  public static int getColumnIdFromName(RelationDef dataDef, String columnName) {
    for (int i = 0; i < dataDef.getColumnsSize(); i++) {
      if (dataDef.getColumnDef(i).getColumnName().equals(columnName)) {
        return i;
      }
    }
    throw new RuntimeException("Column name (" + columnName + ") not found in data document (" + dataDef.getDataPath() + ")");
  }

  /**
   * The target is always right in case of conflict
   * * Merge the columns
   * * Add a primary key if it does not exist
   *
   * @param source
   * @param target
   */
  public static void merge(RelationDef source, RelationDef target) {

    // copy properties
    source.addAllProperties(target);

    // Add the columns
    mergeColumns(source, target);


    // Add the primary key
    if (target.getPrimaryKey() == null) {
      addPrimaryKey(source, target);
    }

    addForeignKeys(source, target);

  }

  /**
   * Add the columns of the source to the target
   * The target may have already some columns. If this is the case,
   * the columns properties will be overwritten by the source.
   * If you don't want
   *
   * @param sourceDef
   * @param targetDef
   */
  public static void mergeColumns(RelationDef sourceDef, RelationDef targetDef) {
    assert sourceDef != null : "SourceDef should not be null";
    assert targetDef != null : "TargetDef should not be null";
    int columnCount = sourceDef.getColumnsSize();
    for (int i = 0; i < columnCount; i++) {
      ColumnDef columnDef = sourceDef.getColumnDef(i);
      ColumnDef targetColumn = targetDef.getColumn(columnDef.getColumnName());
      if (targetColumn == null) {
        targetColumn = targetDef.getOrCreateColumn(columnDef.getColumnName(), columnDef.getClazz())
          .typeCode(columnDef.getDataType().getTypeCode());
      }
      targetColumn
        .precision(columnDef.getPrecision())
        .scale(columnDef.getScale())
        .setNullable(columnDef.getNullable())
        .comment(columnDef.getComment())
        .addAllProperties(columnDef);
    }
  }

  /**
   * A wrapper around the {@link #compare(RelationDef, RelationDef)} function
   * to return a boolean
   *
   * @param leftDataDef
   * @param rightDataDef
   * @return true if there is no diff, false otherewise
   * You can get the reason with the function {@link #compare(RelationDef, RelationDef)}
   */
  public static Boolean equals(RelationDef leftDataDef, RelationDef rightDataDef) {
    if (compare(leftDataDef, rightDataDef) == null) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * @param leftDataDef
   * @param rightDataDef
   * @return null if there is no diff otherwise the reason
   * Compare only the data structure (not the constraints)
   */
  public static String compare(RelationDef leftDataDef, RelationDef rightDataDef) {
    StringBuilder reason = new StringBuilder();

    // Length
    int sourceSize = leftDataDef.getColumnsSize();
    int targetSize = rightDataDef.getColumnsSize();
    if (sourceSize != targetSize) {
      reason.append("The number of columns are not equals. The source data set has ");
      reason.append(sourceSize);
      reason.append(" columns, and the target data set has ");
      reason.append(targetSize);
      reason.append(" columns.");
      reason.append(System.getProperty("line.separator"));
    }


    // Type
    for (int i = 0; i < sourceSize; i++) {
      ColumnDef sourceColumn = leftDataDef.getColumnDef(i);
      ColumnDef targetColumn = rightDataDef.getColumnDef(i);
      if (sourceColumn.getDataType().getTypeCode() != targetColumn.getDataType().getTypeCode()) {
        reason.append("The type column of the column (");
        reason.append(i);
        reason.append(") are not equals. The source column (");
        reason.append(sourceColumn.getColumnName());
        reason.append(") has the type (");
        reason.append(sourceColumn.getDataType().getTypeNames());
        reason.append(") whereas the target column (");
        reason.append(targetColumn.getColumnName());
        reason.append(") has the column type (");
        reason.append(targetColumn.getDataType().getTypeNames());
        reason.append(").");
        reason.append(System.getProperty("line.separator"));
      }
    }
    if (reason.length() == 0) {
      return null;
    } else {
      return reason.toString();
    }
  }
}
