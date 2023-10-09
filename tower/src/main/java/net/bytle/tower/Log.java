package net.bytle.tower;

import net.bytle.fs.Fs;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Log {

  public static final String LOG_DIR_NAME = "logs";
  public static Path LOG_DIR_PATH;

  static {
    Path logDirPath = Paths.get(".")
      .resolve(LOG_DIR_NAME)
      .normalize()
      .toAbsolutePath();
    Fs.createDirectoryIfNotExists(logDirPath);
    LOG_DIR_PATH = logDirPath;
  }

}
