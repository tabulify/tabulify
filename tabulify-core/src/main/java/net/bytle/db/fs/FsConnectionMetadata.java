package net.bytle.db.fs;

import net.bytle.db.connection.ConnectionMetadata;

public class FsConnectionMetadata extends ConnectionMetadata {

  public FsConnectionMetadata(FsConnection fsConnection) {
    super(fsConnection);
  }

  @Override
  public Integer getMaxWriterConnection() {
    return 1;
  }

}
