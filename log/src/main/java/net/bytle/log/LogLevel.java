package net.bytle.log;

import java.util.logging.Level;

public class LogLevel extends Level {

  protected LogLevel(String name, int value) {
    super(name, value);
  }

  static Level get(){
    return new LogLevel("tip",2);
  }

}
