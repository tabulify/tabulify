package net.bytle.db.transfer;

import net.bytle.db.connection.Connection;

public enum TransferType {

  /**
   * Operation executed by the {@link Connection datastore}
   * Local to the datastore
   */
  LOCAL,
  /**
   * Single Thread Cross Transfer between two different data store
   * (Ie also called Stream Transfer because they used {@link net.bytle.db.stream.Stream streams}).
   * Specifically:
   *   * {@link net.bytle.db.stream.InsertStream})
   *   * {@link net.bytle.db.stream.SelectStream})
   */
  SINGLE_CROSS,
  /**
   * A producer or consumer thread will advertise it
   */
  THREAD_CROSS;



}
