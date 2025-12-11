package com.tabulify.flow.stream;

import com.tabulify.Tabular;
import com.tabulify.flow.engine.PipelineStepSupplierDataPath;
import com.tabulify.spi.DataPath;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This is based on the JVM Stream
 *
 */
public class PipelineStream {

  /**
   * The start of a stream
   */
  public static Stream<DataPath> createFrom(Tabular tabular, PipelineStepSupplierDataPath supplier) {
    return StreamSupport.stream(new PipelineSpliterator(supplier), false);
  }


}
