package com.tabulify.flow.operation;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.flow.engine.PipelineStep;
import com.tabulify.flow.engine.PipelineStepBuilderTarget;
import com.tabulify.spi.DropTruncateAttribute;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.type.Casts;
import net.bytle.type.Enums;
import net.bytle.type.KeyNormalizer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.tabulify.flow.operation.DropPipelineStepArgument.PROCESSING_TYPE;

public class DropPipelineStep extends PipelineStepBuilderTarget {

  static final KeyNormalizer DROP = KeyNormalizer.createSafe("drop");


  Set<DropTruncateAttribute> dropAttributes = new HashSet<>();
  private PipelineStepProcessingType processingType = (PipelineStepProcessingType) PROCESSING_TYPE.getDefaultValue();


  public DropPipelineStep setWithForce(Boolean withForce) {
    if (withForce) {
      this.dropAttributes.add(DropTruncateAttribute.FORCE);
    } else {
      this.dropAttributes.remove(DropTruncateAttribute.FORCE);
    }
    return this;
  }

  public static DropPipelineStep builder() {

    return new DropPipelineStep();

  }

  @Override
  public DropPipelineStep createStepBuilder() {
    return new DropPipelineStep();
  }

  @Override
  public List<Class<? extends ArgumentEnum>> getArgumentEnums() {
    return List.of(DropPipelineStepArgument.class);
  }

  @Override
  public PipelineStep build() {
    if (processingType == PipelineStepProcessingType.STREAM) {
      return new DropPipelineStream(this);
    }
    return new DropPipelineBatch(this);
  }

  @Override
  public DropPipelineStep setArgument(KeyNormalizer key, Object value) {

    DropPipelineStepArgument selectArgument;
    try {
      selectArgument = Casts.cast(key, DropPipelineStepArgument.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The argument (" + key + ") is not a valid argument for the step (" + this + "). You can choose one of: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(DropPipelineStepArgument.class));
    }
    Attribute attribute;
    try {
      attribute = this.getPipeline().getTabular().getVault()
        .createVariableBuilderFromAttribute(selectArgument)
        .setOrigin(Origin.PIPELINE)
        .build(value);
      this.setArgument(attribute);
    } catch (CastException e) {
      throw new IllegalArgumentException("The " + selectArgument + " value (" + value + ") of the step (" + this + ") is not conform . Error: " + e.getMessage(), e);
    }

    switch (selectArgument) {
      case FORCE:
        this.setWithForce(attribute.getValueOrDefaultCastAsSafe(Boolean.class));
        break;
      case CASCADE:
        this.setWithCascade(attribute.getValueOrDefaultCastAsSafe(Boolean.class));
        break;
      case PROCESSING_TYPE:
        this.setProcessingType((PipelineStepProcessingType) attribute.getValueOrDefault());
        break;
      default:
        throw new InternalException("The " + selectArgument + " value (" + value + ") of the step (" + this + ") was not processed");
    }
    return this;
  }

  public DropPipelineStep setWithCascade(Boolean withCascade) {
    if (withCascade) {
      this.dropAttributes.add(DropTruncateAttribute.CASCADE);
    } else {
      this.dropAttributes.remove(DropTruncateAttribute.CASCADE);
    }
    return this;
  }

  public DropPipelineStep setProcessingType(PipelineStepProcessingType processingType) {
    this.processingType = processingType;
    return this;
  }

  @Override
  public KeyNormalizer getOperationName() {
    return DROP;
  }


}
