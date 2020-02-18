package net.bytle.db.gen;

import net.bytle.db.gen.generator.CollectionGenerator;
import net.bytle.db.gen.generator.DerivedCollectionGenerator;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.stream.SelectStreamAbs;

import java.sql.Clob;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GenSelectStream extends SelectStreamAbs {

  private final DataGenerator dataGenerator;
  private final GenDataPath genDataPath;
  long actualRowId = 0;

  // For each column def, it's value (ie a row)
  private HashMap<ColumnDef, Object> row;

  public GenSelectStream(GenDataPath dataPath) {

    super(dataPath);
    this.genDataPath = dataPath;
    dataGenerator = DataGenerator.of(genDataPath);

  }

  /**
   * Recursive function that create data for each column for a row
   * The function is recursive to be able to handle direct relationship between columns (ie derived generator)
   *
   * @param columnDef
   * @param columnValues
   */
  private void populateColumnValues(Map<ColumnDef, Object> columnValues, ColumnDef columnDef) {

    if (columnValues.get(columnDef) == null) {

      CollectionGenerator collectionGenerator = dataGenerator.getCollectionGenerator(columnDef);

      if (collectionGenerator.getClass().equals(DerivedCollectionGenerator.class)) {
        DerivedCollectionGenerator dataGeneratorDerived = (DerivedCollectionGenerator) collectionGenerator;
        ColumnDef parentColumn = dataGeneratorDerived.getParentGenerator().getColumn();
        // The column value of the parent must be generated before
        populateColumnValues(columnValues, parentColumn);
      }
      if (collectionGenerator.getColumns().size() == 1) {
        columnValues.put(columnDef, collectionGenerator.getNewValue());
      } else {
        columnValues.put(columnDef, collectionGenerator.getNewValue(columnDef));
      }

    }

  }

  @Override
  public boolean next() {
    if (actualRowId >= dataGenerator.getMaxSize()) {
      return false;
    } else {
      actualRowId++;
      buildRow();
      return true;
    }
  }

  private void buildRow() {
    row = new HashMap<>();
    for (ColumnDef columnDef : genDataPath.getDataDef().getColumnDefs()) {
      populateColumnValues(row, columnDef);
    }

    List<Object> values = new ArrayList<>();
    for (ColumnDef columnDef : genDataPath.getDataDef().getColumnDefs()) {
      // We need also a recursion here to create the value
      values.add(row.get(columnDef));
    }
  }

  @Override
  public void close() {

  }

  @Override
  public String getString(int columnIndex) {
    return null;
  }

  @Override
  public int getRow() {
    return 0;
  }

  @Override
  public Object getObject(int columnIndex) {
    return null;
  }

  @Override
  public TableDef getSelectDataDef() {
    return null;
  }

  @Override
  public double getDouble(int columnIndex) {
    return 0;
  }

  @Override
  public Clob getClob(int columnIndex) {
    return null;
  }

  @Override
  public boolean next(Integer timeout, TimeUnit timeUnit) {
    return false;
  }

  @Override
  public List<Object> getObjects() {
    return Arrays.stream(genDataPath.getDataDef().getColumnDefs())
      .map(c -> row.get(c))
      .collect(Collectors.toList());
  }

  @Override
  public Integer getInteger(int columnIndex) {
    return null;
  }

  @Override
  public Object getObject(String columnName) {
    return null;
  }

  @Override
  public void beforeFirst() {
    throw new RuntimeException("Not yet supported for data generation path");
  }

  @Override
  public void execute() {

  }


}
