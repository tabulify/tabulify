package net.bytle.db.stream;

import net.bytle.db.spi.DataPath;

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
