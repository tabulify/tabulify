package com.tabulify.flow.operation;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.flow.Granularity;
import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.flow.engine.PipelineStepBuilder;
import com.tabulify.flow.engine.PipelineStepIntermediateOneToManyAbs;
import com.tabulify.spi.DataPath;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.template.TextTemplateEngine;
import net.bytle.type.Casts;
import net.bytle.type.Enums;
import net.bytle.type.KeyNormalizer;

import java.util.List;

public class SplitPipelineStep extends PipelineStepIntermediateOneToManyAbs {

  private final SplitPipelineStepBuilder splitBuilder;

  public SplitPipelineStep(SplitPipelineStepBuilder pipelineStepBuilder) {
    super(pipelineStepBuilder);
    this.splitBuilder = pipelineStepBuilder;
  }

  @Override
  public SplitPipelineStepSupplier apply(DataPath dataPath) {

    if (splitBuilder.granularity != Granularity.RECORD) {
      throw new UnsupportedOperationException("Split by " + splitBuilder.granularity + " for the resource " + dataPath + " is not yet supported");
    }

    return (SplitPipelineStepSupplier) SplitPipelineStepSupplier
      .builder()
      .setDataPath(dataPath)
      .setSplitStep(this)
      .setPipeline(this.getPipeline())
      .build();

  }

  public SplitPipelineStepBuilder getSplitBuilder() {
    return splitBuilder;
  }

  static SplitPipelineStepBuilder builder() {
    return new SplitPipelineStepBuilder();
  }


  public static class SplitPipelineStepBuilder extends PipelineStepBuilder {

    static final KeyNormalizer SPLIT = KeyNormalizer.createSafe("split");


    private String targetTemplate;
    // logical record split
    // one resource in another resource
    // example:
    // * html file with 2 tables
    // * xml/json file split by nodes
    private Granularity granularity = (Granularity) SplitPipelineStepArgument.GRANULARITY.getDefaultValue();


    @Override
    public PipelineStepBuilder createStepBuilder() {
      return new SplitPipelineStepBuilder();
    }


    @Override
    public SplitPipelineStepBuilder setArgument(KeyNormalizer key, Object value) {
      SplitPipelineStepArgument argument;
      try {
        argument = Casts.cast(key, SplitPipelineStepArgument.class);
      } catch (CastException e) {
        throw new IllegalArgumentException("The " + key + " value (" + value + ") of the step (" + this + ") is not known. We were expecting one of: " + Enums.toConstantAsStringCommaSeparated(SplitPipelineStepArgument.class), e);
      }
      Attribute attribute;
      try {
        attribute = this.getTabular().getVault()
          .createVariableBuilderFromAttribute(argument)
          .setOrigin(Origin.PIPELINE)
          .build(value);
        this.setArgument(attribute);
      } catch (CastException e) {
        throw new IllegalArgumentException("The " + argument + " value (" + value + ") of the step (" + this + ") is not conform . Error: " + e.getMessage(), e);
      }
      switch (argument) {
        case TARGET_TEMPLATE:
          this.setTargetTemplate(value.toString());
          break;
        case GRANULARITY:
          this.setGranularity(attribute.getValueOrDefaultCastAsSafe(Granularity.class));
          break;
        default:
          throw new InternalException("The argument `" + key + "` should be processed for the step (" + this + ")");
      }
      return this;

    }

    private SplitPipelineStepBuilder setGranularity(Granularity granularity) {
      this.granularity = granularity;
      return this;
    }

    @Override
    public SplitPipelineStep build() {
      return new SplitPipelineStep(this);
    }

    @Override
    public List<Class<? extends ArgumentEnum>> getArgumentEnums() {
      return List.of(SplitPipelineStepArgument.class);
    }

    @Override
    public KeyNormalizer getOperationName() {
      return SPLIT;
    }

    public SplitPipelineStepBuilder setTargetTemplate(String targetPattern) {
      if (!TextTemplateEngine.isTextTemplate(targetPattern)) {
        throw new IllegalArgumentException("The string (" + targetPattern + ") is not a pattern");
      }
      this.targetTemplate = targetPattern;
      return this;
    }

    public String getTargetTemplate() {
      return this.targetTemplate;
    }

  }
}
