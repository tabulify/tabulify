package net.bytle.db.gen;

import net.bytle.db.gen.generator.CollectionGenerator;
import net.bytle.db.gen.generator.CollectionGeneratorMultiple;
import net.bytle.db.gen.generator.CollectionGeneratorOnce;
import net.bytle.db.gen.generator.DerivedCollectionGenerator;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.stream.SelectStreamAbs;
import net.bytle.type.Typess;

import java.sql.Clob;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GenSelectStream extends SelectStreamAbs {

  private final GenDataPath genDataPath;
  long actualRowId = 0;

  // For each column def, it's value (ie a row)
  private HashMap<ColumnDef, Object> row;

  public GenSelectStream(GenDataPath dataPath) {

    super(dataPath);
    this.genDataPath = dataPath;
    this.genDataPath.getDataDef().buildMissingGenerators();

  }

  /**
   * Recursive function that create data for each column for a row
   * The function is recursive to be able to handle direct relationship between columns (ie derived generator)
   *
   * @param columnDef
   * @param columnValues
   */
  private void populateColumnValues(Map<ColumnDef, Object> columnValues, GenColumnDef columnDef) {

    if (columnValues.get(columnDef) == null) {

      CollectionGenerator collectionGenerator = columnDef.getGenerator();

      if (collectionGenerator.getClass().equals(DerivedCollectionGenerator.class)) {
        DerivedCollectionGenerator dataGeneratorDerived = (DerivedCollectionGenerator) collectionGenerator;
        GenColumnDef parentColumn = dataGeneratorDerived.getParentGenerator().getColumn();
        // The column value of the parent must be generated before
        populateColumnValues(columnValues, parentColumn);
      }
      if (collectionGenerator instanceof CollectionGeneratorOnce) {
        columnValues.put(columnDef, ((CollectionGeneratorOnce) collectionGenerator).getNewValue());
      } else {
        columnValues.put(columnDef, ((CollectionGeneratorMultiple) collectionGenerator).getNewValue(columnDef));
      }

    }

  }

  @Override
  public boolean next() {
    if (actualRowId >= genDataPath.getDataDef().getMaxSize()) {
      return false;
    } else {
      actualRowId++;
      buildRow();
      return true;
    }
  }

  private void buildRow() {
    row = new HashMap<>();
    for (GenColumnDef columnDef : genDataPath.getDataDef().getColumnDefs()) {
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
    return Typess.safeCast(getObject(columnIndex),String.class);
  }

  @Override
  public int getRow() {
    return 0;
  }

  @Override
  public Object getObject(int columnIndex) {
    return row.keySet().stream()
      .filter(c -> c.getColumnPosition().equals(columnIndex))
      .map(c->row.get(c))
      .findFirst()
      .orElse(null);
  }

  @Override
  public TableDef getSelectDataDef() {
    return null;
  }

  @Override
  public Double getDouble(int columnIndex) {

    return Typess.safeCast(getObject(columnIndex),Double.class);

  }

  @Override
  public Clob getClob(int columnIndex) {

    return Typess.safeCast(getObject(columnIndex),Clob.class);

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
    return Typess.safeCast(getObject(columnIndex),Integer.class);
  }

  @Override
  public Object getObject(String columnName) {
    return row.keySet().stream()
      .filter(c -> c.getColumnName().equals(columnName))
      .findFirst()
      .orElse(null);
  }

  @Override
  public void beforeFirst() {
    throw new RuntimeException("Not yet supported for data generation path");
  }

  @Override
  public void execute() {

  }


}
