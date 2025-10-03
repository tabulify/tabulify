package com.tabulify.flow.operation;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.flow.engine.PipelineStep;
import com.tabulify.flow.engine.PipelineStepBuilder;
import com.tabulify.spi.DropTruncateAttribute;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.type.Casts;
import net.bytle.type.Enums;
import net.bytle.type.KeyNormalizer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TruncatePipelineStepBuilder extends PipelineStepBuilder {

  public static final KeyNormalizer TRUNCATE = KeyNormalizer.createSafe("truncate");


  private PipelineStepProcessingType processingType = PipelineStepProcessingType.BATCH;
  Set<DropTruncateAttribute> truncateAttributes = new HashSet<>();

  @Override
  public TruncatePipelineStepBuilder createStepBuilder() {
    return new TruncatePipelineStepBuilder();
  }

  @Override
  public List<Class<? extends ArgumentEnum>> getArgumentEnums() {
    return List.of(TruncatePipelineStepArgument.class);
  }

  @Override
  public PipelineStep build() {
    if (this.processingType == PipelineStepProcessingType.BATCH) {
      return new TruncatePipelineStepBatch(this);
    }
    return new TruncatePipelineStepStream(this);
  }

  @Override
  public TruncatePipelineStepBuilder setArgument(KeyNormalizer key, Object value) {

    TruncatePipelineStepArgument truncateArgument;
    try {
      truncateArgument = Casts.cast(key, TruncatePipelineStepArgument.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The argument (" + key + ") is not an valid argument of the operation (" + this + "). We were expecting one of: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TruncatePipelineStepArgument.class));
    }
    Attribute attribute;
    try {
      attribute = this.getPipeline().getTabular().getVault()
        .createVariableBuilderFromAttribute(truncateArgument)
        .setOrigin(Origin.PIPELINE)
        .build(value);
      this.setArgument(attribute);
    } catch (CastException e) {
      throw new IllegalArgumentException("The " + truncateArgument + " value (" + value + ") of the step (" + this + ") is not conform . Error: " + e.getMessage(), e);
    }
    switch (truncateArgument) {
      case FORCE:
        this.setWithForce((Boolean) attribute.getValueOrDefault());
        break;
      case CASCADE:
        this.setWithCascade((Boolean) attribute.getValueOrDefault());
        break;
      case PROCESSING_TYPE:
        this.setProcessingType((PipelineStepProcessingType) attribute.getValueOrDefault());
        break;
      default:
        throw new InternalException("The argument (" + key + ") of the step (" + this + ") was not processed");
    }
    return this;
  }

  public TruncatePipelineStepBuilder setProcessingType(PipelineStepProcessingType pipelineStepProcessingType) {
    this.processingType = pipelineStepProcessingType;
    return this;
  }

  @Override
  public KeyNormalizer getOperationName() {
    return TRUNCATE;
  }

  public TruncatePipelineStepBuilder setWithForce(Boolean withForce) {
    if (withForce) {
      this.truncateAttributes.add(DropTruncateAttribute.FORCE);
    } else {
      this.truncateAttributes.remove(DropTruncateAttribute.FORCE);
    }
    return this;
  }

  public PipelineStepBuilder setWithCascade(Boolean withCascade) {
    if (withCascade) {
      this.truncateAttributes.add(DropTruncateAttribute.CASCADE);
    } else {
      this.truncateAttributes.remove(DropTruncateAttribute.CASCADE);
    }
    return this;
  }
}
