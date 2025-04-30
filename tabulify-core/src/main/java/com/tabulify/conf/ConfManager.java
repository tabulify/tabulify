package com.tabulify.conf;

import com.tabulify.TabularAttribute;
import com.tabulify.Vault;
import net.bytle.exception.CastException;
import net.bytle.fs.Fs;
import net.bytle.type.Casts;
import net.bytle.type.Enums;
import net.bytle.type.Origin;
import net.bytle.type.Variable;
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


  private final Vault vault;
  private final Path path;

  private final Map<TabularAttribute, Variable> env = new HashMap<>();

  public ConfManager(Path path, Vault vault) {

    if (path == null) {
      throw new IllegalArgumentException("The configuration file path passed is null");
    }

    List<String> yamlExtensions = Arrays.asList("yml", "yaml");
    if (!yamlExtensions.contains(Fs.getExtension(path))) {

      throw new IllegalArgumentException("The configuration file (" + path + ") should be a yaml file with the extension (" + String.join(", ", yamlExtensions) + " )");

    }
    this.path = path;
    this.vault = vault;
    try {
      parseYaml();
    } catch (CastException e) {
      // we cannot recover from that
      throw new RuntimeException(e.getMessage(), e);
    }

  }

  public static ConfManager createFromPath(Path confPath, Vault vault) {

    return new ConfManager(confPath, vault);
  }

  public Variable getVariable(TabularAttribute name) {
    return this.env.get(name);
  }


  /**
   * Parse the {@link #path} if the file exists and is not null
   */
  private void parseYaml() throws CastException {


    if (!Files.exists(path)) {
      return;
    }
    /*
     * Read the file into yamlDocuments
     */
    Yaml yaml = new Yaml();
    Iterable<Object> yamlDocuments;
    try {
      yamlDocuments = yaml.loadAll(Files.newBufferedReader(path));
    } catch (Exception e) {
      throw new CastException("Error parsing the Yaml file (" + path + ")", e);
    }

    /**
     * Map processing
     */
    int count = 0;
    for (Object data : yamlDocuments) {

      if (count > 1) {
        throw new CastException("The yaml file (" + path + ") has more than one Yaml document and that's not supported.");
      }
      count++;

      /*
       * Cast
       */
      Map<String, Object> confMap;
      try {
        confMap = Casts.castToNewMap(data, String.class, Object.class);
      } catch (CastException e) {
        throw new RuntimeException("Error: " + e.getMessage() + ". " + badMapCast(data, "map"));
      }
      for (Map.Entry<String, Object> rootEntry : confMap.entrySet()) {

        String rootName = rootEntry.getKey();
        ConfManagerAttribute rootAttribute;
        try {
          rootAttribute = Casts.cast(rootName, ConfManagerAttribute.class);
        } catch (CastException e) {
          throw new CastException("The root name (" + rootName + ") is not valid. We were expecting one of " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(ConfManagerAttribute.class), e);
        }

        switch (rootAttribute) {
          case VARIABLES:

            Map<String, String> localEnvs;
            try {
              localEnvs = Casts.castToSameMap(rootEntry.getValue(), String.class, String.class);
            } catch (CastException e) {
              throw new CastException("Error: " + e.getMessage() + ". " + badMapCast(data, String.valueOf(ConfManagerAttribute.VARIABLES)), e);
            }
            for (Map.Entry<String, String> localEnv : localEnvs.entrySet()) {

              TabularAttribute tabularAttribute;
              String variableName = localEnv.getKey();
              try {
                tabularAttribute = Casts.cast(variableName, TabularAttribute.class);
              } catch (ClassCastException e) {
                throw new CastException("The env name (" + variableName + ") is not valid. We were expecting one of " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TabularAttribute.class), e);
              }
              Variable variable = vault.createVariableWithRawValue(tabularAttribute, localEnv.getValue(), Origin.USER);
              env.put(tabularAttribute, variable);

            }

            break;
        }

      }

    }


  }

  private static String badMapCast(Object data, String keyPath) {
    String message = "The " + keyPath + " configuration must be in a map format. ";
    if (data.getClass().equals(ArrayList.class)) {
      message += "They are in a list format. You should suppress the minus as name suffix if they are present.";
    }
    message += "The Bad Data Values are: " + data;
    return message;

  }

  public void close() {

    this.flush();

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

      Map<String, Object> confMap = new HashMap<>();
      for (Map.Entry<String, Object> entry : confMap
        .entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .collect(Collectors.toList())) {

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

  /**
   * Used in Tabli
   */
  public void addVariable(String key, Object value) throws CastException {
    TabularAttribute tabularAttribute;
    try {
      tabularAttribute = Casts.cast(key, TabularAttribute.class);
    } catch (CastException e) {
      throw new CastException("Error: the variable name (" + key + " is not a valid variable name. We were expecting one of: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TabularAttribute.class));
    }
    Variable variable = vault.createVariableWithRawValue(tabularAttribute, value, Origin.USER);
    env.put(tabularAttribute, variable);
  }
}
