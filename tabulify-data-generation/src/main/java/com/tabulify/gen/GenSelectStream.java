package com.tabulify.gen;

import com.tabulify.gen.generator.CollectionGenerator;
import com.tabulify.model.ColumnDef;
import com.tabulify.stream.SelectStreamAbs;
import net.bytle.type.Casts;

import java.sql.Clob;
import java.util.HashMap;
import java.util.List;
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


  }


  @Override
  public boolean next() {
    if (actualRowId >= genDataPath.getCount()) {
      return false;
    } else {
      actualRowId++;
      buildRow();
      return true;
    }
  }

  private void buildRow() {
    row = new HashMap<>();
    List<CollectionGenerator<?>> collectionGenerators = CollectionGenerator.createDag()
      .addRelations(
        genDataPath.getOrCreateRelationDef()
          .getAllColumnDefs()
          .stream()
          .map(c -> (c.getOrCreateGenerator(c.getClazz()))
            .setColumnDef(c))
          .collect(Collectors.toList())
      )
      .getCreateOrdered();
    for(CollectionGenerator<?> c: collectionGenerators){
      GenColumnDef columnDef = c.getColumnDef();
      Object newValue = c.getNewValue();
      row.put(columnDef, newValue);
    }

  }

  @Override
  public void close() {
    genDataPath.getOrCreateRelationDef()
      .getColumnDefs()
      .forEach(c ->
        c.getOrCreateGenerator(c.getClazz())
          .reset()
      );
  }

  @Override
  public String getString(int columnIndex) {
    return String.valueOf(getObject(columnIndex));
  }

  @Override
  public long getRow() {
    return actualRowId;
  }

  @Override
  public Object getObject(int columnIndex) {
    if (actualRowId == 0) {
      throw new RuntimeException("You are on the row 0, you need to use the next function before retrieving a value");
    }

    ColumnDef columnDef = row.keySet().stream()
      .filter(c -> c.getColumnPosition().equals(columnIndex))
      .findFirst()
      .orElse(null);

    /**
     * Because the value generated may be null
     * we don't get the value in the stream
     */
    return row.get(columnDef);


  }


  @Override
  public Double getDouble(int columnIndex) {

    return Casts.castSafe(getObject(columnIndex), Double.class);

  }

  @Override
  public Clob getClob(int columnIndex) {

    return Casts.castSafe(getObject(columnIndex), Clob.class);

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
  public Integer getInteger(int columnIndex) {
    return Casts.castSafe(getObject(columnIndex), Integer.class);
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


}
