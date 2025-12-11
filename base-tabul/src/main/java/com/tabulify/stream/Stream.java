package com.tabulify.stream;

import com.tabulify.spi.DataPath;

public interface Stream {


  /**
   * @return the data path Definition
   */
  DataPath getDataPath();

  /**
   * The name of the stream (The identifier used in the log for instance)
   * @return
   */
  String getName();


}
