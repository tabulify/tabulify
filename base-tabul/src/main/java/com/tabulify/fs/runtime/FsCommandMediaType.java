package com.tabulify.fs.runtime;

import com.tabulify.type.MediaType;

public enum FsCommandMediaType implements MediaType {

  COMMAND_MEDIA_TYPE("file-system", "command");

  private final String type;
  private final String sub;

  FsCommandMediaType(String type, String subtype) {
    this.type = type;
    this.sub = subtype;
  }

  @Override
  public String getSubType() {
    return this.sub;
  }

  @Override
  public String getType() {
    return this.type;
  }

  @Override
  public boolean isContainer() {
    return false;
  }

  @Override
  public boolean isRuntime() {
    return true;
  }

  @Override
  public String getExtension() {
    return "";
  }

  @Override
  public String toString() {
    return type + '/' + sub;
  }

}
