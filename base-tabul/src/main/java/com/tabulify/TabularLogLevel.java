package com.tabulify;


import java.util.logging.Level;

public enum TabularLogLevel {

  ERROR(Level.SEVERE),
  WARN(Level.WARNING),
  TIP(new TabularLogCustomLevel("TIP", 850)),
  // Log of cli, step operations
  INFO(Level.INFO),
  // Log of data path operation (drop, ...)
  FINE(Level.FINE),
  // Log of attribute, cache
  FINEST(Level.FINEST);


  private final Level level;

  TabularLogLevel(Level level) {
    this.level = level;
  }

  public Level getLevel() {
    return level;
  }
}
