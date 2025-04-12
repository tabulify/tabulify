package com.tabulify.memory;

import com.tabulify.connection.ConnectionMetadata;

public class MemoryConnectionMetadata extends ConnectionMetadata {
  public MemoryConnectionMetadata(MemoryConnection memoryConnection) {
    super(memoryConnection);
  }

  @Override
  public Integer getMaxWriterConnection() {
    return 1;
  }

}
