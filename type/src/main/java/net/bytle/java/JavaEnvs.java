package net.bytle.java;

import net.bytle.type.env.OsEnvs;

import java.nio.file.NotDirectoryException;
import java.nio.file.Path;

public class JavaEnvs {

  public static final Boolean IS_DEV;

  public static final Path HOME_PATH;
  public static final boolean IS_IDE_DEBUGGING;

  static {

    /**
     * Home path and is Dev mode
     */
    Path sourceCodePath = Javas.getSourceCodePath(JavaEnvs.class);
    Path homePath = sourceCodePath.getParent().getParent();
    Boolean isDev = false;

    /**
     * For vertx, the Dev mode is :
     * * with the VERTXWEB_ENVIRONMENT environment variable
     * * or vertxweb.environment system property
     * set to dev.
     */
    String env = OsEnvs.getEnvOrDefault("VERTXWEB_ENVIRONMENT", "prod");
    if(env.equals("dev")){
      isDev = true;
    }

    try {
      Path buildPath = Javas.getBuildDirectory(JavaEnvs.class);
      homePath = buildPath
        .toAbsolutePath()
        .getParent()
        .getParent();
      isDev = true;
    } catch (NotDirectoryException e) {
      // ok
    }

    /**
     * If the mode is debugging, this value is true
     * (Works in eclipse, idea)
     * It's used mostly to disable the timeout that will otherwise
     * kick you out of a debug session.
     * https://stackoverflow.com/questions/1109019/determine-if-a-java-application-is-in-debug-mode-in-eclipse
     */
    IS_IDE_DEBUGGING = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("-agentlib:jdwp");

    IS_DEV = isDev;
    HOME_PATH = homePath;


  }


  /**
   * This function is not in the file system module to avoid circular dependency
   */
  public static Path getPathUntilName(Path path, String name) {

    Path pathUntil = null;
    boolean found = false;
    for (int i = 0; i < path.getNameCount(); i++) {
      Path subName = path.getName(i);
      if (pathUntil == null) {
        if (path.isAbsolute()) {
          pathUntil = path.getRoot().resolve(subName);
        } else {
          pathUntil = subName;
        }
      } else {
        pathUntil = pathUntil.resolve(subName);
      }
      if (subName.getFileName().toString().equals(name)) {
        found = true;
        break;
      }
    }
    if (found) {
      return pathUntil;
    } else {
      return null;
    }
  }


}
