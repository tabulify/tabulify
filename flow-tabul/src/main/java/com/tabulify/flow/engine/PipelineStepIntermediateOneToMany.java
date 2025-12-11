package com.tabulify.flow.engine;

import com.tabulify.spi.DataPath;

import java.util.function.Function;

/**
 * A step that transforms:
 * * get a data path and produce many
 * <p>
 * Example:
 * * {@link PipelineStepSupplierDataPathAbs supplier}
 * * but also any step that want to output the source or target and result for instance
 */
public interface PipelineStepIntermediateOneToMany extends PipelineStepIntermediateSupplier, Function<DataPath, PipelineStepSupplierDataPath> {


}
