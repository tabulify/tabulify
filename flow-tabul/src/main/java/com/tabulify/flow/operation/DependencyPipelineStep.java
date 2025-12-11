package com.tabulify.flow.operation;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.flow.engine.PipelineStep;
import com.tabulify.flow.engine.PipelineStepBuilder;
import com.tabulify.flow.engine.PipelineStepBuilderTarget;
import com.tabulify.model.RelationDef;
import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataPathAttribute;
import com.tabulify.stream.InsertStream;
import com.tabulify.dag.Dependency;
import com.tabulify.exception.CastException;
import com.tabulify.exception.InternalException;
import com.tabulify.type.Casts;
import com.tabulify.type.Enums;
import com.tabulify.type.KeyNormalizer;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class DependencyPipelineStep extends PipelineStepBuilderTarget {


  private PipelineStepProcessingType processingType = (PipelineStepProcessingType) DependencyPipelineStepArgument.PROCESSING_TYPE.getDefaultValue();

  public static DependencyPipelineStep builder() {

    return new DependencyPipelineStep();

  }

  @Override
  public DependencyPipelineStep setArgument(KeyNormalizer key, Object value) {


    DependencyPipelineStepArgument selectArgument;
    try {
      selectArgument = Casts.cast(key, DependencyPipelineStepArgument.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The argument (" + key + ") is not a valid argument for the step (" + this + "). You can choose one of: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(DependencyPipelineStepArgument.class));
    }

    Tabular tabular = this.getTabular();

    Attribute attribute;
    try {
      attribute = tabular.getVault()
        .createVariableBuilderFromAttribute(selectArgument)
        .setOrigin(Origin.PIPELINE)
        .build(value);
      this.setArgument(attribute);
    } catch (CastException e) {
      throw new IllegalArgumentException("The " + selectArgument + " value (" + value + ") of the step (" + this + ") is not conform . Error: " + e.getMessage(), e);
    }

    switch (selectArgument) {

      case PROCESSING_TYPE:
        try {
          this.setProcessingType(attribute.getValueOrDefaultCastAs(PipelineStepProcessingType.class));
        } catch (CastException e) {
          throw new IllegalArgumentException("The argument (" + key + ") for the step (" + this + ") has a value (" + value + ") that is not valid. Error: " + e.getMessage(), e);
        }
        break;
      default:
        throw new InternalException("The argument (" + key + ") for the step (" + this + ") should have a branch in the switch");
    }

    return this;

  }

  public DependencyPipelineStep setProcessingType(PipelineStepProcessingType processingType) {
    this.processingType = processingType;
    return this;
  }

  @Override
  public List<Class<? extends ArgumentEnum>> getArgumentEnums() {
    ArrayList<Class<? extends ArgumentEnum>> list = new ArrayList<>(super.getArgumentEnums());
    list.add(DependencyPipelineStepArgument.class);
    return list;
  }

  static final KeyNormalizer DEPENDENCY = KeyNormalizer.createSafe("dependency");

  @Override
  public PipelineStepBuilder createStepBuilder() {

    return new DependencyPipelineStep();
  }

  @Override
  public PipelineStep build() {

    if (this.processingType == PipelineStepProcessingType.BATCH) {
      return new DependencyPipelineStepBatch(this);
    }

    return new DependencyPipelineStepStream(this);
  }

  @Override
  public KeyNormalizer getOperationName() {
    return DEPENDENCY;
  }

  public DataPath getDependencyDataPath(List<DataPath> dataPaths) {

    RelationDef feedback;
    // Creating a table to use the print function
    feedback = this.getTabular().getMemoryConnection().getAndCreateRandomDataPath()
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

      for (DataPath dataPath : dataPaths) {
        for (Dependency dependency : dataPath.getDependencies().stream().sorted().collect(toList())) {
          List<Object> row = new ArrayList<>();
          dependenciesNumber++;
          row.add(dependenciesNumber);
          row.add(dataPath.toDataUri().toString());
          row.add(dependency.getId());
          insertStream.insert(row);
        }
      }
    }
    return feedback.getDataPath();

  }
}
