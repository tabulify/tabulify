package com.tabulify.spi;

public enum DropTruncate {

  DROP,
  TRUNCATE;

  @Override
  public String toString() {
    return this.name().toLowerCase();
  }
}
