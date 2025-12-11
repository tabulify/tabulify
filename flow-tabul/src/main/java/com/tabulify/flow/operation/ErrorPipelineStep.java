package com.tabulify.flow.operation;

import com.tabulify.TabularExecEnv;
import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.flow.engine.PipelineStep;
import com.tabulify.flow.engine.PipelineStepBuilder;
import com.tabulify.flow.engine.PipelineStepBuilderTarget;
import com.tabulify.exception.CastException;
import com.tabulify.exception.InternalException;
import com.tabulify.type.Casts;
import com.tabulify.type.Enums;
import com.tabulify.type.KeyNormalizer;

import java.util.List;

/**
 * An internal step that will simulate an exception
 */
public class ErrorPipelineStep extends PipelineStepBuilderTarget {

  public static final KeyNormalizer ERROR = KeyNormalizer.createSafe("error");
  public Integer failEveryNCount = (Integer) ErrorPipelineStepArgument.FAIL_EVERY_N_COUNT.getDefaultValue();
  private PipelineStepProcessingType processingType = (PipelineStepProcessingType) ErrorPipelineStepArgument.PROCESSING_TYPE.getDefaultValue();
  Boolean isEnabled = (Boolean) ErrorPipelineStepArgument.ENABLE.getDefaultValue();
  ErrorPipelineStepFailPoint failPoint = (ErrorPipelineStepFailPoint) ErrorPipelineStepArgument.FAIL_POINT.getDefaultValue();


  public static ErrorPipelineStep builder() {
    return new ErrorPipelineStep();
  }

  @Override
  public PipelineStepBuilder createStepBuilder() {
    return new ErrorPipelineStep();
  }

  @Override
  public PipelineStep build() {
    if (isEnabled == null) {
      isEnabled = this.getTabular().getExecutionEnvironment() != TabularExecEnv.PROD;
    }
    if (!isEnabled) {
      return new ErrorPipelineStepStream(this);
    }
    if (this.processingType == PipelineStepProcessingType.BATCH) {
      return new ErrorPipelineStepBatch(this);
    }
    return new ErrorPipelineStepStream(this);
  }

  @Override
  public List<Class<? extends ArgumentEnum>> getArgumentEnums() {
    return List.of(ErrorPipelineStepArgument.class);
  }

  @Override
  public KeyNormalizer getOperationName() {
    return ERROR;
  }

  @Override
  public ErrorPipelineStep setArgument(KeyNormalizer key, Object value) {

    ErrorPipelineStepArgument selectArgument;
    try {
      selectArgument = Casts.cast(key, ErrorPipelineStepArgument.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The argument (" + key + ") is not a valid argument for the step (" + this + "). You can choose one of: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(ErrorPipelineStepArgument.class));
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
      case PROCESSING_TYPE:
        this.setProcessingType((PipelineStepProcessingType) attribute.getValueOrDefault());
        break;
      case FAIL_EVERY_N_COUNT:
        this.setFailEveryNCount((Integer) attribute.getValueOrDefault());
        break;
      case ENABLE:
        this.setIsEnabled((Boolean) attribute.getValueOrDefault());
        break;
      case FAIL_POINT:
        this.setFailPoint((ErrorPipelineStepFailPoint) attribute.getValueOrDefault());
        break;
      default:
        throw new InternalException("The " + selectArgument + " value (" + value + ") of the step (" + this + ") was not processed");
    }
    return this;
  }

  public ErrorPipelineStep setFailPoint(ErrorPipelineStepFailPoint failPoint) {
    this.failPoint = failPoint;
    return this;
  }

  private ErrorPipelineStep setIsEnabled(Boolean enabled) {
    this.isEnabled = enabled;
    return this;
  }

  public ErrorPipelineStep setFailEveryNCount(Integer failEveryNCount) {
    this.failEveryNCount = failEveryNCount;
    return this;
  }


  public ErrorPipelineStep setProcessingType(PipelineStepProcessingType processingType) {
    this.processingType = processingType;
    return this;
  }
}
