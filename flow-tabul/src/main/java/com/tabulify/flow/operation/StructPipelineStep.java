package com.tabulify.flow.operation;

import com.tabulify.flow.engine.PipelineStep;
import com.tabulify.flow.engine.PipelineStepBuilder;
import com.tabulify.flow.engine.PipelineStepIntermediateMapAbs;
import com.tabulify.model.ColumnAttribute;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.PrimaryKeyDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataPathAttribute;
import com.tabulify.stream.InsertStream;
import com.tabulify.type.KeyNormalizer;

import java.util.List;

/**
 * Transform a data path in a struct data path (ie columns definitions)
 */
public class StructPipelineStep extends PipelineStepIntermediateMapAbs {


  public StructPipelineStep(StructPipelineStepBuilder pipelineStepBuilder) {
    super(pipelineStepBuilder);
  }

  public static StructPipelineStepBuilder create() {

    return new StructPipelineStepBuilder();
  }

  @Override
  public DataPath apply(DataPath dataPath) {

    DataPath structDataPath = dataPath.getConnection().getTabular().getMemoryConnection()
      .getAndCreateRandomDataPath()
      .setLogicalName("columns")
      .getOrCreateRelationDef()
      .getDataPath();


    structDataPath.setComment("Structure of the resource " + dataPath.toDataUri());

    // May be used later in a concat step
    structDataPath.addAttribute(DataPathAttribute.DATA_URI, dataPath.toDataUri());


    structDataPath.getOrCreateRelationDef()
      .addColumn(KeyNormalizer.createSafe(ColumnAttribute.POSITION).toSqlCase())
      .addColumn(KeyNormalizer.createSafe(ColumnAttribute.NAME).toSqlCase())
      .addColumn(KeyNormalizer.createSafe(ColumnAttribute.TYPE).toSqlCase())
      .addColumn(KeyNormalizer.createSafe(ColumnAttribute.PRECISION).toSqlCase())
      .addColumn(KeyNormalizer.createSafe(ColumnAttribute.SCALE).toSqlCase())
      .addColumn("primary_key")
      .addColumn(KeyNormalizer.createSafe(ColumnAttribute.NULLABLE).toSqlCase())
      .addColumn(KeyNormalizer.createSafe(ColumnAttribute.AUTOINCREMENT).toSqlCase())
      .addColumn(KeyNormalizer.createSafe(ColumnAttribute.COMMENT).toSqlCase());


    try (
      InsertStream insertStream = structDataPath.getInsertStream()
    ) {

      List<ColumnDef<?>> columnDefs = dataPath.getOrCreateRelationDef().getColumnDefs();
      for (ColumnDef<?> columnDef : columnDefs) {

        PrimaryKeyDef primaryKey = dataPath.getOrCreateRelationDef().getPrimaryKey();
        Object[] columnsColumns = {
          columnDef.getColumnPosition(),
          columnDef.getColumnName(),
          columnDef.getDataType().getParentOrSelf().toKeyNormalizer().toSqlTypeCase(),
          columnDef.getPrecision(),
          columnDef.getScale(),
          (primaryKey != null ? (primaryKey.getColumns().contains(columnDef) ? "x" : "") : ""),
          (columnDef.isNullable() ? "x" : ""),
          columnDef.isAutoincrement(),
          columnDef.getComment()};

        insertStream.insert(columnsColumns);
      }

    }

    return structDataPath;

  }


  public static class StructPipelineStepBuilder extends PipelineStepBuilder {

    static KeyNormalizer STRUCT = KeyNormalizer.createSafe("struct");


    @Override
    public PipelineStepBuilder createStepBuilder() {
      return new StructPipelineStepBuilder();
    }

    @Override
    public PipelineStep build() {
      return new StructPipelineStep(this);
    }

    @Override
    public KeyNormalizer getOperationName() {
      return STRUCT;
    }
  }
}
