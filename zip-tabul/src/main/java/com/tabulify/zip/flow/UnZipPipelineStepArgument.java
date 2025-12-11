package com.tabulify.zip.flow;

import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.flow.engine.PipelineStepBuilderTargetArgument;
import com.tabulify.flow.operation.StepOutputArgument;
import com.tabulify.flow.operation.StreamType;
import com.tabulify.uri.DataUriStringNode;
import com.tabulify.type.KeyNormalizer;

public enum UnZipPipelineStepArgument implements ArgumentEnum {


  ENTRY_SELECTOR("A glob pattern. If set, only the archive entries that match will be extracted", null, DataUriStringNode.class),
  TARGET_DATA_URI("A target data uri template that defines the destination directory where the archive entry are extracted", "${entry_path}@tmp", PipelineStepBuilderTargetArgument.TARGET_DATA_URI.getValueClazz()),
  STRIP_COMPONENTS("Number of parts striped from the entry path to calculate the destination relative path from the destination directory (equivalent to strip-components in tar)", 0, Integer.class),
  OUTPUT_TYPE("The output type", StepOutputArgument.RESULTS, StepOutputArgument.class),
  STREAM_TYPE("The stream type", StreamType.MAP, StreamType.class);


  private final Object defaultValue;
  private final String description;
  private final Class<?> clazz;

  UnZipPipelineStepArgument(String description, Object defaultValue, Class<?> clazz) {

    this.description = description;
    this.defaultValue = defaultValue;
    this.clazz = clazz;

  }


  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.clazz;
  }

  public Object getDefaultValue() {
    return this.defaultValue;
  }

  @Override
  public String toString() {
    return KeyNormalizer.createSafe(name()).toCliLongOptionName();
  }

}
