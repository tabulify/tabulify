package com.tabulify.fs;

import com.tabulify.connection.ConnectionMetadata;

public class FsConnectionMetadata extends ConnectionMetadata {

  public FsConnectionMetadata(FsConnection fsConnection) {
    super(fsConnection);
  }

  @Override
  public Integer getMaxWriterConnection() {
    return 1;
  }

}
