package com.tabulify.conf;

import com.tabulify.Tabular;
import com.tabulify.TabularAttribute;
import com.tabulify.Vault;
import com.tabulify.connection.Connection;
import com.tabulify.connection.ConnectionAttribute;
import net.bytle.exception.CastException;
import net.bytle.fs.Fs;
import net.bytle.type.*;
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
  private final Tabular tabular;
  private final MapKeyIndependent<Connection> connections = new MapKeyIndependent<>();

  /**
   * @param path    - the file
   * @param vault   - the vault to create the variables
   * @param tabular - tabular to create the connection
   */
  public ConfManager(Path path, Vault vault, Tabular tabular) {

    if (path == null) {
      throw new IllegalArgumentException("The configuration file path passed is null");
    }

    List<String> yamlExtensions = Arrays.asList("yml", "yaml");
    if (!yamlExtensions.contains(Fs.getExtension(path))) {

      throw new IllegalArgumentException("The configuration file (" + path + ") should be a yaml file with the extension (" + String.join(", ", yamlExtensions) + " )");

    }
    this.path = path;
    this.vault = vault;
    this.tabular = tabular;
    try {
      parseYaml();
    } catch (CastException e) {
      // we cannot recover from that
      throw new RuntimeException(e.getMessage(), e);
    }

  }

  public static ConfManager createFromPath(Path confPath, Vault vault, Tabular tabular) {

    return new ConfManager(confPath, vault, tabular);
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
        ConfManagerRootAttribute rootAttribute;
        try {
          rootAttribute = Casts.cast(rootName, ConfManagerRootAttribute.class);
        } catch (CastException e) {
          throw new CastException("The root name (" + rootName + ") is not valid. We were expecting one of " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(ConfManagerRootAttribute.class), e);
        }

        switch (rootAttribute) {
          case VARIABLES:

            Map<String, String> localEnvs;
            try {
              localEnvs = Casts.castToSameMap(rootEntry.getValue(), String.class, String.class);
            } catch (CastException e) {
              throw new CastException("Error: " + e.getMessage() + ". " + badMapCast(data, String.valueOf(ConfManagerRootAttribute.VARIABLES)), e);
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
          case CONNECTIONS:

            Map<String, Object> localConnections;
            try {
              localConnections = Casts.castToSameMap(rootEntry.getValue(), String.class, Object.class);
            } catch (CastException e) {
              throw new CastException("Error: " + e.getMessage() + ". " + badMapCast(data, String.valueOf(ConfManagerRootAttribute.CONNECTIONS)), e);
            }
            Variable uri = null;
            Set<Variable> variableMap = new SetKeyIndependent<>();
            for (Map.Entry<String, Object> localConnection : localConnections.entrySet()) {

              String connectionName = localConnection.getKey();
              Map<String, Object> connectionAttributes = Casts.castToSameMap(localConnection.getValue(), String.class, Object.class);
              for (Map.Entry<String, Object> confConnectionAttribute : connectionAttributes.entrySet()) {

                String connectionAttributeAsString = confConnectionAttribute.getKey();
                ConnectionAttribute connectionAttribute;
                try {
                  connectionAttribute = Casts.cast(connectionAttributeAsString, ConnectionAttribute.class);
                } catch (Exception e) {
                  throw new CastException("The connection attribute (" + connectionAttributeAsString + ") is not valid. We were expecting one of " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(ConnectionAttribute.class), e);
                }
                if (connectionAttribute == ConnectionAttribute.DRIVER) {
                  continue;
                }

                Variable variable;
                try {
                  variable = vault.createVariableWithRawValue(connectionAttribute, confConnectionAttribute.getValue(), Origin.USER);

                } catch (Exception e) {
                  throw new RuntimeException("An error has occurred while reading the connection attribute " + connectionAttributeAsString + " value for the connection (" + connectionName + "). Error: " + e.getMessage(), e);
                }
                if (connectionAttribute == ConnectionAttribute.URI) {
                  uri = variable;
                  continue;
                }
                variableMap.add(variable);
              }

              if (uri == null) {
                throw new RuntimeException("The uri is a mandatory variable and was not found for the connection (" + connectionName + ") in the conf file (" + this + ")");
              }

              /**
               * Create the connection
               */
              Connection connection = Connection.createConnectionFromProviderOrDefault(this.tabular, connectionName, (String) uri.getValueOrDefaultOrNull());
              // variables map should be in the building of the connection
              // as they may be used for the default values
              connection.setVariables(variableMap);
              connection.addVariable(vault.createVariableSafe(ConnectionAttribute.ORIGIN, Origin.USER, Origin.INTERNAL));
              connections.put(connectionName, connection);

            }

            break;
          default:
            throw new RuntimeException("Internal Error: the root attribute " + rootAttribute + " should be processed");
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

  @Override
  public String toString() {
    return path.toAbsolutePath().toString();
  }

  public Connection getConnection(String name) {
    return this.connections.get(name);
  }

}
