package com.tabulify.conf;

import net.bytle.fs.Fs;
import net.bytle.type.Casts;
import net.bytle.type.MapKeyIndependent;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A class that manages a configuration file
 */
public class ConfManager implements AutoCloseable {


  private Path path;
  private Map<String, Object> confMap = new MapKeyIndependent<>();

  public ConfManager(Path path) {
    this.setPath(path);
  }

  public static ConfManager createFromPath(Path confPath) {
    return new ConfManager(confPath);
  }

  public Map<String, Object> getConfMap() {

    return confMap;
  }


  public ConfManager setPath(Path path) {

    if (path == null) {
      throw new IllegalArgumentException("The configuration file path passed is null");
    }

    List<String> yamlExtensions = Arrays.asList("yml", "yaml");
    if (!yamlExtensions.contains(Fs.getExtension(path))) {

      throw new IllegalArgumentException("The configuration file (" + path + ") should be a yaml file with the extension (" + String.join(", ", yamlExtensions) + " )");

    } else {

      this.path = path;
      parseYaml();

    }
    return this;
  }

  /**
   * Parse the {@link #path} if the file exists and is not null
   * and save the result into {@link #confMap}
   */
  private void parseYaml() {
    if (this.path != null) {
      /*
       * Parsing
       */
      if (Files.exists(path)) {
        /*
         * Read the file into the map
         */
        try {
          Yaml yaml = new Yaml();
          int count = 0;
          for (Object data : yaml.loadAll(Files.newBufferedReader(path))) {

            if (count > 1) {
              throw new RuntimeException("The yaml file (" + path + ") has more than one Yaml document and that's not supported.");
            }
            count++;

            /*
             * Cast
             */
            try {
              confMap = Casts.castToSameMap(data, String.class, Object.class);
            } catch (ClassCastException e) {
              String message = e.getMessage() + ". A configuration must be in a map format. ";
              if (data.getClass().equals(java.util.ArrayList.class)) {
                message += "They are in a list format. You should suppress the minus as name suffix if they are present.";
              }
              message += "The Bad Data Values are: " + data;
              throw new RuntimeException(message, e);
            }
          }
        } catch (Exception e) {
          throw new RuntimeException("Error parsing the Yaml file (" + path + ")", e);
        }
      }
    }
  }

  public void close() {

    this.flush();

  }

  public ConfManager setProperty(String key, String value) {
    this.getConfMap().put(key, value);
    return this;
  }


  public ConfManager reset() {
    try {
      Files.deleteIfExists(this.path);
      this.confMap = new HashMap<>();
      return this;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void flush() {
    BufferedWriter outputStream;

    try {

      if (!Files.exists(path)) {
        Fs.createEmptyFile(path);
      }
      /*
       * Snake YML does not permit to add comments
       * We are then writing the yaml text file ourselves
       */
      outputStream = Files.newBufferedWriter(path);

      for (Map.Entry<String, Object> entry : getConfMap().entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {

        // Value
        Object value = entry.getValue();
        if (value instanceof Collection) {
          outputStream.write(entry.getKey() + ":");
          outputStream.newLine();
          Collection<?> collectionValue = (Collection<?>) value;
          for (Object colValue : collectionValue) {
            outputStream.write("  - " + colValue);
            outputStream.newLine();
          }
        } else {
          outputStream.write(entry.getKey() + ": " + entry.getValue());
          outputStream.newLine();
        }
        // Hr
        outputStream.newLine();
      }
      outputStream.flush();
      outputStream.close();

    } catch (IOException e) {

      throw new RuntimeException(e);

    }
  }

  public ConfManager reload() {

    parseYaml();

    return this;
  }

  public Object delete(String key) {

    return confMap.remove(key);
  }
}
