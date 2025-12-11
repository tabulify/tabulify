package com.tabulify.transfer;

import com.tabulify.connection.Connection;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.stream.Stream;

public enum TransferType {

  /**
   * Operation executed by the {@link Connection datastore}
   * Local to the datastore
   */
  LOCAL,
  /**
   * Single Thread Cross Transfer between two different data store
   * (Ie also called Stream Transfer because they used {@link Stream streams}).
   * Specifically:
   *   * {@link InsertStream})
   *   * {@link SelectStream})
   */
  SINGLE_CROSS,
  /**
   * A producer or consumer thread will advertise it
   */
  THREAD_CROSS;



}
