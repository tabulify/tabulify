package com.tabulify.gen.flow.fill;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.flow.engine.PipelineStepBuilder;
import com.tabulify.uri.DataUriNode;
import com.tabulify.uri.DataUriStringNode;
import com.tabulify.exception.CastException;
import com.tabulify.exception.InternalException;
import com.tabulify.type.Casts;
import com.tabulify.type.Enums;
import com.tabulify.type.KeyNormalizer;

import java.util.ArrayList;
import java.util.List;

/**
 * The fill step will create or select data generators
 * from a stream of inputs
 */
public class FillPipelineStep extends PipelineStepBuilder {

  private static final KeyNormalizer FILL = KeyNormalizer.createSafe("fill");


  List<DataUriNode> generatorSelectorList = new ArrayList<>();
  long maxRecordCount = (long) FillPipelineStepArgument.MAX_RECORD_COUNT.getDefaultValue();

  public static FillPipelineStep builder() {
    return new FillPipelineStep();
  }


  @Override
  public List<Class<? extends ArgumentEnum>> getArgumentEnums() {
    return List.of(FillPipelineStepArgument.class);
  }


  @Override
  public FillPipelineStep createStepBuilder() {
    return new FillPipelineStep();
  }


  @Override
  public FillPipelineStepBatch build() {
    return new FillPipelineStepBatch(this);
  }

  @Override
  public FillPipelineStep setArgument(KeyNormalizer key, Object value) {

    FillPipelineStepArgument fillArgument;
    try {
      fillArgument = Casts.cast(key, FillPipelineStepArgument.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The argument (" + key + ") is not valid argument for the step (" + this + "). We were expecting " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(FillPipelineStepArgument.class));
    }
    Attribute attribute;
    try {
      attribute = this.getTabular().getVault()
        .createVariableBuilderFromAttribute(fillArgument)
        .setOrigin(Origin.PIPELINE)
        .build(value);
    } catch (CastException e) {
      throw new IllegalArgumentException("The argument value (" + fillArgument + ") is not valid for the step (" + this + "). Error: " + e.getMessage(), e);
    }
    this.setArgument(attribute);

    switch (fillArgument) {

      case GENERATOR_SELECTOR:
        DataUriStringNode generatorSelector;
        try {
          generatorSelector = DataUriStringNode.createFromString(value.toString());
          this.generatorSelectorList.add(this.getPipelineBuilder().getDataUri(generatorSelector));
        } catch (CastException e) {
          throw new IllegalArgumentException("The argument value (" + fillArgument + ") is not valid for the step (" + this + "). Error: " + e.getMessage(), e);
        }
        break;
      case GENERATOR_SELECTORS:
        try {
          List<String> generatorSelectors = Casts.castToNewList(value, String.class);
          List<DataUriNode> generatorSelectorsDataUriList = new ArrayList<>();
          for (String generatorSelectorString : generatorSelectors) {
            generatorSelectorsDataUriList.add(
              this.getPipelineBuilder().getDataUri(
                DataUriStringNode.createFromString(generatorSelectorString))
            );
          }
          this.generatorSelectorList.addAll(generatorSelectorsDataUriList);
        } catch (CastException e) {
          throw new IllegalArgumentException("The argument value (" + fillArgument + ") is not valid for the step (" + this + "). Error: " + e.getMessage(), e);
        }
        break;
      case MAX_RECORD_COUNT:
        this.setMaxRecordCount((long) attribute.getValueOrDefault());
        break;
      default:
        throw new InternalException("The fill Argument (" + fillArgument + ") was not implemented");

    }
    return this;

  }

  public FillPipelineStep setMaxRecordCount(long maxRecordCount) {
    this.maxRecordCount = maxRecordCount;
    return this;
  }

  public FillPipelineStep setGeneratorSelectors(List<DataUriNode> generatorSelector) {
    this.generatorSelectorList = generatorSelector;
    return this;
  }


  @Override
  public KeyNormalizer getOperationName() {
    return FILL;
  }

}
