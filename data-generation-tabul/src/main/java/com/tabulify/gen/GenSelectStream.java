package com.tabulify.gen;

import com.tabulify.gen.generator.CollectionGenerator;
import com.tabulify.model.ColumnDef;
import com.tabulify.stream.SelectStreamAbs;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GenSelectStream extends SelectStreamAbs {

  private final GenDataPath genDataPath;
  private final List<CollectionGenerator<?>> generators;

  long actualRowId = 0;

  // For each column def, it's value (ie a row)
  private Map<ColumnDef<?>, Object> row;

  public GenSelectStream(GenDataPath dataPath) {

    super(dataPath);
    this.genDataPath = dataPath;
    generators = this.genDataPath.getOrCreateRelationDef().buildGeneratorInCreateOrder();

  }


  @Override
  public boolean next() {

    if (actualRowId >= genDataPath.getCount()) {
      return false;
    }
    actualRowId++;
    row = this.genDataPath.getOrCreateRelationDef().buildRowFromGenerators(generators);
    return true;

  }


  @Override
  public void close() {
    this.isClosed = true;
    genDataPath.getOrCreateRelationDef()
      .getColumnDefs()
      .forEach(c ->
        c.getOrCreateGenerator()
          .reset()
      );
  }

  private boolean isClosed = false;

  @Override
  public boolean isClosed() {
    return isClosed;
  }

  @Override
  public String getString(int columnIndex) {
    return String.valueOf(getObject(columnIndex));
  }

  @Override
  public long getRecordId() {
    return actualRowId;
  }


  @Override
  public Object getObject(ColumnDef columnDef) {

    if (actualRowId == 0) {
      throw new RuntimeException("You are on the row 0, you need to use the next function before retrieving a value");
    }

    /**
     * Because the value generated may be null
     * we don't get the value in the stream
     */
    return row.get(columnDef);

  }


  @Override
  public boolean next(Integer timeout, TimeUnit timeUnit) {
    return false;
  }

  @Override
  public List<?> getObjects() {
    return genDataPath
      .getOrCreateRelationDef().getColumnDefs()
      .stream()
      .map(c -> row.get(c))
      .collect(Collectors.toList());
  }


  @Override
  public void beforeFirst() {
    throw new RuntimeException("Not yet supported for data generation path");
  }


}
