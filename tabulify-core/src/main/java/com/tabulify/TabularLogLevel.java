package com.tabulify;

import net.bytle.log.Log;

import java.util.logging.Level;

public enum TabularLogLevel {

  ERROR(Level.SEVERE),
  WARN(Level.WARNING),
  TIP(Log.TIP),
  INFO(Level.INFO),
  FINE(Level.FINE);


  private final Level level;

  TabularLogLevel(Level level) {
    this.level = level;
  }

  public Level getLevel() {
    return level;
  }
}
