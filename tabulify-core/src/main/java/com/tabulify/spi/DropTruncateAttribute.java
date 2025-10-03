package com.tabulify.spi;

public enum DropTruncateAttribute {

  FORCE,
  CASCADE,
  IF_EXISTS;


  @Override
  public String toString() {
    return this.name().toLowerCase();
  }
}
