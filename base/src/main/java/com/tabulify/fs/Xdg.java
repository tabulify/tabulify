package com.tabulify.fs;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Cross implementation of
 * <a href="https://specifications.freedesktop.org/basedir-spec/latest/">...</a>
 */
@SuppressWarnings("unused")
public class Xdg {


  /**
   * @param appName - the app name
   * @return XDG_DATA_HOME
   * where user-specific data files should be stored.
   */
  public static Path getDataHome(String appName) {

    String os = System.getProperty("os.name").toLowerCase();

    if (os.contains("win")) {
      // Windows
      String appData = System.getenv("LOCALAPPDATA");
      if (appData == null) {
        appData = System.getProperty("user.home") + "\\AppData\\Local";
      }
      return Paths.get(appData, appName);

    }
    if (os.contains("mac")) {
      // macOS
      return Paths.get(System.getProperty("user.home"), "Library");
    }


    // Linux/Unix
    String xdgDataHome = System.getenv("XDG_DATA_HOME");
    if (xdgDataHome == null) {
      xdgDataHome = System.getProperty("user.home") + "/.local/share";
    }
    return Paths.get(xdgDataHome, appName);


  }

  /**
   * @return the runtime directory or the temporary
   */
  public static Path getRuntimeTemporaryDir(String appName){
    String xdgRuntimeDir = System.getenv("XDG_RUNTIME_DIR");
    if (xdgRuntimeDir == null) {
      return Fs.getTempDirectory().resolve(appName);
    }
    return Paths.get(xdgRuntimeDir).resolve(appName);
  }

}
