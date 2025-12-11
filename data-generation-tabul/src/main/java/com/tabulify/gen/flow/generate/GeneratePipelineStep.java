package com.tabulify.gen.flow.generate;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.flow.Granularity;
import com.tabulify.flow.engine.*;
import com.tabulify.flow.operation.PipelineStepProcessingType;
import com.tabulify.gen.GenDataPath;
import com.tabulify.spi.DataPath;
import com.tabulify.uri.DataUriNode;
import com.tabulify.exception.CastException;
import com.tabulify.exception.InternalException;
import com.tabulify.type.Casts;
import com.tabulify.type.KeyNormalizer;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class GeneratePipelineStep extends PipelineStepBuilderStreamSupplier {

  private final List<GenDataPath> genDataPaths = new ArrayList<>();

  private Long streamRecordCount = (Long) GeneratePipelineStepArgument.STREAM_RECORD_COUNT.getDefaultValue();
  private List<DataUriNode> dataSelectors = new ArrayList<>();
  private Boolean strictSelection = (Boolean) GeneratePipelineStepArgument.STRICT_SELECTION.getDefaultValue();
  private PipelineStepProcessingType processingType = (PipelineStepProcessingType) GeneratePipelineStepArgument.PROCESSING_TYPE.getDefaultValue();
  private Granularity streamGranularity = (Granularity) GeneratePipelineStepArgument.STREAM_GRANULARITY.getDefaultValue();


  @Override
  public GeneratePipelineStep createStepBuilder() {
    return new GeneratePipelineStep();
  }

  @Override
  public PipelineStep build() {

    List<DataPath> dataPathSetSelected = this.getTabular().select(dataSelectors, strictSelection, null);
    for (DataPath dataPath : dataPathSetSelected) {
      try {
        this.genDataPaths.add(Casts.cast(dataPath, GenDataPath.class));
      } catch (CastException e) {
        throw new IllegalArgumentException("The data resource (" + dataPath + ") is not a generator");
      }
    }

    if (processingType == PipelineStepProcessingType.BATCH) {
      return new GeneratePipelineStepBatch(this);
    }

    return new GeneratePipelineStepStream(this);
  }

  @Override
  public KeyNormalizer getOperationName() {
    return GeneratePipelineStepStream.GENERATE;
  }

  public PipelineStepBuilder addGenerator(GenDataPath dataPath) {
    this.genDataPaths.add(dataPath);
    return this;
  }

  @Override
  public List<Class<? extends ArgumentEnum>> getArgumentEnums() {
    ArrayList<Class<? extends ArgumentEnum>> list = new ArrayList<>(super.getArgumentEnums());
    list.add(GeneratePipelineStepArgument.class);
    return list;
  }

  public List<GenDataPath> getGenDataPaths() {
    return this.genDataPaths;
  }


  public Long getStreamRecordCount() {
    return this.streamRecordCount;
  }

  @Override
  public GeneratePipelineStep setArgument(KeyNormalizer key, Object value) {


    GeneratePipelineStepArgument generateArgument;
    Class<GeneratePipelineStepArgument> targetClass = GeneratePipelineStepArgument.class;
    try {
      generateArgument = Casts.cast(key, targetClass);
    } catch (CastException e) {
      /**
       * May be a {@link PipelineArgument}
       */
      return (GeneratePipelineStep) super.setArgument(key, value);
    }

    Tabular tabular = this.getPipeline().getTabular();

    Attribute attribute;
    try {
      attribute = tabular.getVault()
        .createVariableBuilderFromAttribute(generateArgument)
        .setOrigin(Origin.PIPELINE)
        .build(value);
      this.setArgument(attribute);
    } catch (CastException e) {
      throw new IllegalArgumentException("The " + generateArgument + " value (" + value + ") of the step (" + this + ") is not conform . Error: " + e.getMessage(), e);
    }

    switch (generateArgument) {
      case DATA_SELECTOR:
        this.setDataSelector(tabular.createDataUri(attribute.getValueOrDefaultAsStringNotNull()));
        break;
      case DATA_SELECTORS:
        List<DataUriNode> dataUris;
        try {
          dataUris = Casts.castToNewList(attribute.getValueOrDefault(), String.class)
            .stream()
            .map(tabular::createDataUri)
            .collect(toList());
        } catch (CastException e) {
          throw new IllegalArgumentException("The argument (" + key + ") for the step (" + this + ") has a value (" + value + ") that is not valid. Error: " + e.getMessage(), e);
        }
        this.setDataSelectors(dataUris);
        break;
      case STRICT_SELECTION:
        this.setStrictSelection(attribute.getValueOrDefaultCastAsSafe(Boolean.class));
        break;
      case STREAM_RECORD_COUNT:
        this.setStreamRecordCount((Long) attribute.getValueOrDefault());
        break;
      case PROCESSING_TYPE:
        this.setProcessingType((PipelineStepProcessingType) attribute.getValueOrDefault());
        break;
      case STREAM_GRANULARITY:
        this.setStreamGranularity((Granularity) attribute.getValueOrDefault());
      default:
        throw new InternalException("The argument (" + key + ") for the step (" + this + ") should have a branch in the switch");
    }

    return this;

  }

  private GeneratePipelineStep setStreamGranularity(Granularity valueOrDefault) {
    this.streamGranularity = valueOrDefault;
    return this;
  }

  private GeneratePipelineStep setProcessingType(PipelineStepProcessingType processingType) {
    this.processingType = processingType;
    return this;
  }

  private GeneratePipelineStep setStrictSelection(Boolean strictSelection) {
    this.strictSelection = strictSelection;
    return this;
  }

  private GeneratePipelineStep setDataSelectors(List<DataUriNode> dataUris) {
    this.dataSelectors = dataUris;
    return this;
  }

  private GeneratePipelineStep setStreamRecordCount(Long streamRecordCount) {
    this.streamRecordCount = streamRecordCount;
    return this;
  }

  private GeneratePipelineStep setDataSelector(DataUriNode dataUri) {
    this.dataSelectors = List.of(dataUri);
    return this;
  }

  public PipelineStepProcessingType getProcessingType() {
    return this.processingType;
  }

  public Granularity getStreamGranularity() {
    return this.streamGranularity;
  }
}
