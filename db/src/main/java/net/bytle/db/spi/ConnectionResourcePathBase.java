package net.bytle.db.spi;

import net.bytle.db.DbLoggers;

/**
 * A string path without path functionality
 */
public class ConnectionResourcePathBase extends ConnectionResourcePathAbs {

  public ConnectionResourcePathBase(String glob) {
    super(glob);
    DbLoggers.LOGGER_DB_ENGINE.fine("This string path does not implement a real path system");
  }

  @Override
  public ResourcePath toAbsolute() {
    return this;
  }

  @Override
  public Boolean isAbsolute() {
    return true;
  }

  @Override
  public ResourcePath normalize() {
    return this;
  }

}
