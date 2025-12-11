package com.tabulify.gen.flow.enrich;

import com.tabulify.flow.engine.PipelineStepIntermediateMapAbs;
import com.tabulify.spi.DataPath;

public class EnrichPipelineStepMap extends PipelineStepIntermediateMapAbs {


  private final EnrichPipelineStep enrichBuilder;

  public EnrichPipelineStepMap(EnrichPipelineStep pipelineStepBuilder) {
    super(pipelineStepBuilder);
    this.enrichBuilder = pipelineStepBuilder;
  }


  @Override
  public DataPath apply(DataPath dataPath) {

    if (this.enrichBuilder.yamlMap.isEmpty()) {
      return dataPath;
    }

    EnrichDataPath enrichDataPath = EnrichDataPath.create(dataPath);

    /**
     * Add the virtual column
     */
    enrichDataPath.mergeDataDefinitionFromYamlMap(this.enrichBuilder.yamlMap);

    return enrichDataPath;

  }


}
