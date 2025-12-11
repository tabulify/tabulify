package com.tabulify.flow.engine;

import com.tabulify.spi.DataPath;

import java.util.function.Function;

/**
 * A map function (one input, one output)
 */
public interface PipelineStepIntermediateMap extends PipelineStepIntermediate, Function<DataPath, DataPath> {


}
