package net.bytle.api;

import net.bytle.fs.Fs;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Log {

  public static final String LOG_DIR_NAME = "logs";
  static Path LOG_DIR_PATH;

  static {
    Path logDirPath = Paths.get(Paths.get(".").toString(),LOG_DIR_NAME);
    Fs.createDirectoryIfNotExists(logDirPath);
    LOG_DIR_PATH = logDirPath;
  }

}
