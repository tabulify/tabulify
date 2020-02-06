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
 * Retrieve a list of TableDef through a Data Definition file
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
  public static void mergeProperties(TableDef firstTable, TableDef secondTable) {

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


  public static TableDef of(DataPath dataPath) {

    return dataPath.getDataDef();

  }

  /**
   * Add the columns to the targetDef from the sourceDef
   *
   * @param sourceDef
   * @param targetDef
   */
  public static void addColumns(RelationDef sourceDef, RelationDef targetDef) {

    assert sourceDef != null : "SourceDef should not be null";
    int columnCount = sourceDef.getColumnDefs().size();
    for (int i = 0; i < columnCount; i++) {
      ColumnDef columnDef = sourceDef.getColumnDef(i);
      targetDef.getColumnOf(columnDef.getColumnName(), columnDef.getClazz())
        .typeCode(columnDef.getDataType().getTypeCode())
        .precision(columnDef.getPrecision())
        .scale(columnDef.getScale());
    }


  }

  public static DataPath getColumnsDataPath(Tabular tabular, List<DataPath> dataPaths) {

    DataPath columnsInfoDataPath = tabular.getDataPath("columns")
      .getDataDef()
      .getDataPath();

    if (dataPaths.size() >= 1) {
      columnsInfoDataPath.getDataDef()
        .addColumn("#")
        .addColumn("Table Name");
    }
    columnsInfoDataPath.getDataDef()
      .addColumn("Position")
      .addColumn("Column Name")
      .addColumn("Data Type")
      .addColumn("Primary Key")
      .addColumn("Not Null")
      .addColumn("Default")
      .addColumn("Auto Increment")
      .addColumn("Description");

    try (
      InsertStream insertStream = Tabulars.getInsertStream(columnsInfoDataPath);
    ) {
      int i = 0;
      for (DataPath dataPath : dataPaths) {
        for (ColumnDef columnDef : dataPath.getDataDef().getColumnDefs()) {

          Object[] columnsColumns = {
            columnDef.getColumnPosition(),
            columnDef.getColumnName(),
            columnDef.getDataType().getTypeName(),
            (dataPath.getDataDef().getPrimaryKey().getColumns().contains(columnDef) ? "x" : ""),
            (columnDef.getNullable() ? "x" : ""),
            columnDef.getDefault(),
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


  public static void copy(TableDef source, TableDef target) {

    if (source == null) {
      return;
    }

    // Add the columns
    addColumns(source, target);

    // Add the primary key
    final PrimaryKeyDef sourcePrimaryKey = source.getPrimaryKey();
    if (sourcePrimaryKey != null) {
      final List<String> columns = sourcePrimaryKey.getColumns().stream()
        .map(s -> s.getColumnName())
        .collect(Collectors.toList());
      target.setPrimaryKey(columns);
    }

    // Add the foreign key if the tables exist
    final List<ForeignKeyDef> foreignKeyDefs = source.getForeignKeys();
    for (ForeignKeyDef foreignKeyDef : foreignKeyDefs) {
      DataPath sourceForeignDataPath = foreignKeyDef.getForeignPrimaryKey().getDataDef().getDataPath();
      DataPath targetForeignDataPath = target.getDataPath().getSibling(sourceForeignDataPath.getName());
      // Does the table exist in the target
      if (Tabulars.exists(targetForeignDataPath)) {
        PrimaryKeyDef targetPrimaryKey = targetForeignDataPath.getDataDef().getPrimaryKey();
        if (targetPrimaryKey != null) {
          List<String> targetForeignPrimaryKeyColumns = targetPrimaryKey.getColumns().stream().map(ColumnDef::getColumnName).collect(Collectors.toList());
          List<String> sourceForeignPrimaryKeyColumns = sourceForeignDataPath.getDataDef().getPrimaryKey().getColumns().stream().map(ColumnDef::getColumnName).collect(Collectors.toList());
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
          logger.warn("Foreign Key not copied: The target data path (" + targetForeignDataPath + ") exists but does not have any primary key");
        }
      } else {
        logger.warn("Foreign Key not copied: The target data path (" + targetForeignDataPath + ") does not exist");
      }
    }
  }

  public static int getColumnIdFromName(TableDef dataDef, String columnName) {
    for (int i = 0; i < dataDef.getColumnDefs().size(); i++) {
      if (dataDef.getColumnDef(i).getColumnName().equals(columnName)) {
        return i;
      }
    }
    throw new RuntimeException("Column name (" + columnName + ") not found in data document (" + dataDef.getDataPath() + ")");
  }
}
