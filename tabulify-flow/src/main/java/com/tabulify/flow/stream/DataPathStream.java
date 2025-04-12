package com.tabulify.flow.stream;

import com.tabulify.spi.DataPath;

import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DataPathStream {

  /**
   * The start of a stream
   * @return
   */
  public static Stream<Set<DataPath>> createFrom(DataPathSupplier supplier) {
    return StreamSupport.stream(new DataPathSpliterator(supplier), false);
  }


}
