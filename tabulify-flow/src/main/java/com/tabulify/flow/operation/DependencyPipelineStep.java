package com.tabulify.flow.operation;

import com.tabulify.flow.engine.PipelineStepBuilder;
import com.tabulify.flow.engine.PipelineStepBuilderTarget;
import com.tabulify.flow.engine.PipelineStepIntermediateMapAbs;
import com.tabulify.model.RelationDef;
import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataPathAttribute;
import com.tabulify.stream.InsertStream;
import net.bytle.dag.Dependency;
import net.bytle.type.KeyNormalizer;

import java.util.ArrayList;
import java.util.List;

public class DependencyPipelineStep extends PipelineStepIntermediateMapAbs {

  public DependencyPipelineStep(DependencyPipelineStepBuilder pipeline) {
    super(pipeline);
  }

  public static DependencyPipelineStepBuilder builder() {

    return new DependencyPipelineStepBuilder();
  }



  @Override
  public DataPath apply(DataPath dataPath) {

    RelationDef feedback;
    // Creating a table to use the print function
    feedback = dataPath.getConnection().getTabular().getMemoryConnection().getAndCreateRandomDataPath()
      .setLogicalName("dependencies")
      .getOrCreateRelationDef();
    feedback.addColumn(KeyNormalizer.createSafe("Id").toSqlCase(), SqlDataTypeAnsi.INTEGER);

    feedback
      .addColumn(KeyNormalizer.createSafe(DataPathAttribute.DATA_URI).toSqlCase())
      .addColumn(KeyNormalizer.createSafe("Dependency").toSqlCase());


    try (
      InsertStream insertStream = feedback.getDataPath().getInsertStream()
    ) {
      // Filling the table with data
      Integer dependenciesNumber = 0;


      for (Dependency dependency : dataPath.getDependencies()) {
        List<Object> row = new ArrayList<>();
        dependenciesNumber++;
        row.add(dependenciesNumber);
        row.add(dataPath.toDataUri().toString());
        row.add(dependency.getId());
        insertStream.insert(row);
      }

    }
    return feedback.getDataPath();

  }

  public static class DependencyPipelineStepBuilder extends PipelineStepBuilderTarget {

    static final KeyNormalizer DEPENDENCY = KeyNormalizer.createSafe("dependency");

    @Override
    public PipelineStepBuilder createStepBuilder() {
      return new DependencyPipelineStepBuilder();
    }

    @Override
    public DependencyPipelineStep build() {
      return new DependencyPipelineStep(this);
    }

    @Override
    public KeyNormalizer getOperationName() {
      return DEPENDENCY;
    }
  }
}
