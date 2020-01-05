package net.bytle.api;

import net.bytle.fs.Fs;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Log {

  static Path LOG_DIR_PATH;

  static {
    Path logDirPath = Paths.get(Paths.get(".").toString(),"logs");
    Fs.createDirectoryIfNotExists(logDirPath);
    LOG_DIR_PATH = logDirPath;
  }

}
