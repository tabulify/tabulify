package com.tabulify.flow.step;

import com.tabulify.flow.engine.FilterStepAbs;
import com.tabulify.flow.engine.FilterRunnable;
import com.tabulify.model.ColumnAttribute;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.PrimaryKeyDef;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import net.bytle.type.Arrayss;
import net.bytle.type.KeyNormalizer;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Transform a data path in a struct data path (ie columns definitions)
 */
public class StructStep extends FilterStepAbs implements Function<Set<DataPath>, DataPath> {


  public static StructStep create() {
    return new StructStep();
  }

  @Override
  public DataPath apply(Set<DataPath> dataPaths) {

    DataPath structDataPath = tabular.getMemoryDataStore()
      .getAndCreateRandomDataPath()
      .setLogicalName("columns")
      .getOrCreateRelationDef()
      .getDataPath();

    if (dataPaths.size()==1){
      structDataPath.setDescription("Structure of the resource "+dataPaths.iterator().next().toDataUri());
    } else {
      structDataPath.setDescription("Structure");
    }
    if (dataPaths.size() > 1) {
      structDataPath.getOrCreateRelationDef()
        .addColumn("record_id")
        .addColumn("data_uri");
    }
    structDataPath.getOrCreateRelationDef()
      .addColumn(KeyNormalizer.create(ColumnAttribute.POSITION).toSqlCase())
      .addColumn(KeyNormalizer.create(ColumnAttribute.NAME).toSqlCase())
      .addColumn(KeyNormalizer.create(ColumnAttribute.TYPE).toSqlCase())
      .addColumn(KeyNormalizer.create(ColumnAttribute.PRECISION).toSqlCase())
      .addColumn(KeyNormalizer.create(ColumnAttribute.SCALE).toSqlCase())
      .addColumn("primary_key")
      .addColumn(KeyNormalizer.create(ColumnAttribute.NULLABLE).toSqlCase())
      .addColumn(KeyNormalizer.create(ColumnAttribute.AUTOINCREMENT).toSqlCase())
      .addColumn(KeyNormalizer.create(ColumnAttribute.COMMENT).toSqlCase());

    if (dataPaths.isEmpty()) {
      return structDataPath;
    }

    try (
      InsertStream insertStream = structDataPath.getInsertStream()
    ) {
      int i = 0;
      for (DataPath dataPath : dataPaths.stream().sorted().collect(Collectors.toList())) {
        List<ColumnDef> columnDefs = dataPath.getOrCreateRelationDef().getColumnDefs();
        for (ColumnDef columnDef : columnDefs) {

          PrimaryKeyDef primaryKey = dataPath.getOrCreateRelationDef().getPrimaryKey();
          Object[] columnsColumns = {
            columnDef.getColumnPosition(),
            columnDef.getColumnName(),
            columnDef.getDataType().getSqlName(),
            columnDef.getPrecision(),
            columnDef.getScale(),
            (primaryKey != null ? (primaryKey.getColumns().contains(columnDef) ? "x" : "") : ""),
            (columnDef.isNullable() ? "x" : ""),
            columnDef.isAutoincrement(),
            columnDef.getDescription()};

          if (dataPaths.size() > 1) {
            i++;
            Object[] tablesColumns = {i, dataPath.toDataUri().toString()};
            columnsColumns = Arrayss.concat(tablesColumns, columnsColumns);
          }

          insertStream.insert(columnsColumns);
        }
      }
    }

    return structDataPath;

  }

  @Override
  public String getOperationName() {
    return "struct";
  }



  @Override
  public FilterRunnable createRunnable() {
    return new StructRunner(this);
  }

  private static class StructRunner implements FilterRunnable {
    private final StructStep structOperation;
    private final Set<DataPath> inputs = new HashSet<>();
    private DataPath output;
    private boolean isDone = false;

    public StructRunner(StructStep structOperation) {
      this.structOperation = structOperation;
    }

    @Override
    public void addInput(Set<DataPath> inputs) {
      this.inputs.addAll(inputs);
    }

    @Override
    public void run() {
      this.output = this.structOperation.apply(inputs);
      isDone = true;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      return false;
    }

    @Override
    public boolean isCancelled() {
      return false;
    }

    @Override
    public boolean isDone() {
      return isDone;
    }

    @Override
    public Set<DataPath> get() {
      return Collections.singleton(output);
    }

    @Override
    public Set<DataPath> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return get();
    }
  }

  @Override
  public boolean isAccumulator() {
    return true;
  }
}
