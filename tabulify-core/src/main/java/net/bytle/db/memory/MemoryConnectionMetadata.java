package net.bytle.db.memory;

import net.bytle.db.connection.ConnectionMetadata;

public class MemoryConnectionMetadata extends ConnectionMetadata {
  public MemoryConnectionMetadata(MemoryConnection memoryConnection) {
    super(memoryConnection);
  }

  @Override
  public Integer getMaxWriterConnection() {
    return 1;
  }

}
