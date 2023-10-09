package net.bytle.type.dotenv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Storing configuration in the environment is one of the tenets of a twelve-factor app.
 * Anything that is likely to change between deployment environments–such as resource handles
 * for databases or credentials for external services–should be extracted
 * from the code into environment variables.
 * <p>
 * But it is not always practical to set environment variables
 * on development machines or continuous integration servers
 * where multiple projects are run.
 * Dotenv load variables from a .env file into ENV when the environment is bootstrapped.
 * <p>
 * -- Brandon Keepers
 * https://github.com/cdimascio/dotenv-java
 * based on
 * https://12factor.net/config - twelve factor app
 */
public class DotEnv {


  private final Properties dotenv;

  public DotEnv(Path path) {
    dotenv = new Properties();
    try {
      dotenv.load(Files.newBufferedReader(path));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * If directory is not specified it defaults to searching the current working directory on the filesystem.
   * If not found, it searches the current directory on the classpath.
   *
   * @return the object
   */
  static public DotEnv createFromCurrentDirectory() {

    return new DotEnv(Paths.get(".env"));
  }

  /**
   * path specifies the directory containing .env.
   * Dotenv first searches for .env using the given path on the filesystem.
   * If not found, it searches the given path on the classpath.
   *
   * @return the object
   */
  static public DotEnv createFromPath(Path path) {

    return new DotEnv(path);
  }

  public String get(String key) {
    return this.dotenv.getProperty(key);
  }

  public String get(String key, String defaultValue) {
    return this.dotenv.getProperty(key, defaultValue);
  }

  public Map<String, String> getAll() {
    return this.dotenv.entrySet().stream()
      .collect(Collectors.toMap(
        e -> e.getKey().toString(),
        e -> e.getValue().toString()
      ));
  }

}
