package com.tabulify.flow.operation;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.flow.engine.PipelineStep;
import com.tabulify.flow.engine.PipelineStepBuilder;
import com.tabulify.flow.engine.PipelineStepBuilderTarget;
import com.tabulify.uri.DataUriNode;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.type.Casts;
import net.bytle.type.KeyNormalizer;

import java.util.ArrayList;
import java.util.List;

public class CreatePipelineStep extends PipelineStepBuilderTarget {

  static final KeyNormalizer CREATE_TARGET = KeyNormalizer.createSafe("create");
  private CreatePipelineStepOutputArgument outputType = (CreatePipelineStepOutputArgument) CreatePipelineStepArgument.OUTPUT.getDefaultValue();
  private PipelineStepProcessingType processingType = (PipelineStepProcessingType) CreatePipelineStepArgument.PROCESSING_TYPE.getDefaultValue();

  @Override
  public PipelineStepBuilder createStepBuilder() {
    return new CreatePipelineStep();
  }

  public static CreatePipelineStep builder() {
    return new CreatePipelineStep();
  }

  @Override
  public PipelineStep build() {

    if (processingType == PipelineStepProcessingType.STREAM) {
      return new CreatePipelineStepMap(this);
    }
    return new CreatePipelineStepBatch(this);
  }

  @Override
  public List<Class<? extends ArgumentEnum>> getArgumentEnums() {
    ArrayList<Class<? extends ArgumentEnum>> enums = new ArrayList<>(super.getArgumentEnums());
    enums.add(CreatePipelineStepArgument.class);
    return enums;
  }

  @Override
  public KeyNormalizer getOperationName() {
    return CREATE_TARGET;
  }

  @Override
  public CreatePipelineStep setArgument(KeyNormalizer key, Object value) {
    CreatePipelineStepArgument createArgument;
    try {
      createArgument = Casts.cast(key, CreatePipelineStepArgument.class);
    } catch (CastException e) {
      super.setArgument(key, value);
      return this;
    }
    Attribute attribute;
    try {
      attribute = this.getTabular().getVault()
        .createVariableBuilderFromAttribute(createArgument)
        .setOrigin(Origin.PIPELINE)
        .build(value);
      this.setArgument(attribute);
    } catch (CastException e) {
      throw new IllegalArgumentException("The " + createArgument + " value (" + value + ") of the step (" + this + ") is not conform . Error: " + e.getMessage(), e);
    }

    switch (createArgument) {
      case OUTPUT:
        this.setOutputType((CreatePipelineStepOutputArgument) attribute.getValueOrDefault());
        break;
      case PROCESSING_TYPE:
        this.setProcessingType((PipelineStepProcessingType) attribute.getValueOrDefault());
        break;
      default:
        throw new InternalException("The " + createArgument + " value (" + value + ") of the step (" + this + ") was not taken into account");
    }
    return this;
  }

  private CreatePipelineStep setProcessingType(PipelineStepProcessingType processingType) {
    this.processingType = processingType;
    return this;
  }

  private CreatePipelineStep setOutputType(CreatePipelineStepOutputArgument outputType) {
    this.outputType = outputType;
    return this;
  }

  public DataUriNode getTargetUri() {
    return this.targetUri;
  }

  public CreatePipelineStepOutputArgument getOutput() {
    return this.outputType;
  }



}
