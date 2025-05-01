package com.tabulify.conf;

import com.tabulify.Tabular;
import com.tabulify.TabularAttribute;
import com.tabulify.Vault;
import com.tabulify.connection.Connection;
import com.tabulify.connection.ConnectionAttributeBase;
import com.tabulify.connection.ConnectionOrigin;
import net.bytle.exception.CastException;
import net.bytle.fs.Fs;
import net.bytle.type.*;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * A class that manages a configuration file
 */
public class ConfVault {


  private final Vault vault;
  private final Path path;

  private final Map<TabularAttribute, Variable> env = new HashMap<>();
  private final Tabular tabular;
  private final MapKeyIndependent<Connection> connections = new MapKeyIndependent<>();
  private final KeyCase outputCase = KeyCase.HYPHEN;

  /**
   * @param path    - the file
   * @param vault   - the vault to create the variables
   * @param tabular - tabular to create the connection
   *                All parameters are there to force the initialization order.
   *                ie Vault is reachable from the tabular global object,
   *                but it should be created before confManager
   */
  public ConfVault(Path path, Vault vault, Tabular tabular) {

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
      throw new RuntimeException("Error while parsing the config vault " + this + ". Error: " + e.getMessage(), e);
    }

  }


  public static ConfVault createFromPath(Path confPath, Vault vault, Tabular tabular) {

    return new ConfVault(confPath, vault, tabular);
  }

  public static ConfVault createFromPath(Path confPath, Tabular tabular) {

    return new ConfVault(confPath, tabular.getVault(), tabular);
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
        ConfVaultRootAttribute rootAttribute;
        try {
          rootAttribute = Casts.cast(rootName, ConfVaultRootAttribute.class);
        } catch (CastException e) {
          throw new CastException("The root name (" + rootName + ") is not valid. We were expecting one of " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(ConfVaultRootAttribute.class), e);
        }

        switch (rootAttribute) {
          case VARIABLES:

            Map<String, String> localEnvs;
            try {
              localEnvs = Casts.castToSameMap(rootEntry.getValue(), String.class, String.class);
            } catch (CastException e) {
              throw new CastException("Error: " + e.getMessage() + ". " + badMapCast(data, String.valueOf(ConfVaultRootAttribute.VARIABLES)), e);
            }
            for (Map.Entry<String, String> localEnv : localEnvs.entrySet()) {

              TabularAttribute tabularAttribute;
              String variableName = localEnv.getKey();
              try {
                tabularAttribute = Casts.cast(variableName, TabularAttribute.class);
              } catch (ClassCastException e) {
                throw new CastException("The env name (" + variableName + ") is not valid. We were expecting one of " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TabularAttribute.class), e);
              }
              Variable variable = vault.createVariable(tabularAttribute, localEnv.getValue(), Origin.CONF);
              env.put(tabularAttribute, variable);

            }

            break;
          case CONNECTIONS:

            Map<String, Object> localConnections;
            try {
              localConnections = Casts.castToSameMap(rootEntry.getValue(), String.class, Object.class);
            } catch (CastException e) {
              throw new CastException("Error: " + e.getMessage() + ". " + badMapCast(data, String.valueOf(ConfVaultRootAttribute.CONNECTIONS)), e);
            }
            Variable uri = null;
            Set<Variable> variableMap = new SetKeyIndependent<>();
            Set<Variable> driverVariableMap = new SetKeyIndependent<>();
            for (Map.Entry<String, Object> localConnection : localConnections.entrySet()) {

              String connectionName = localConnection.getKey();
              Map<String, Object> connectionAttributes = Casts.castToSameMap(localConnection.getValue(), String.class, Object.class);
              for (Map.Entry<String, Object> confConnectionAttribute : connectionAttributes.entrySet()) {

                String connectionAttributeAsString = confConnectionAttribute.getKey();
                ConnectionAttributeBase connectionAttributeBase;
                try {
                  connectionAttributeBase = Casts.cast(connectionAttributeAsString, ConnectionAttributeBase.class);
                } catch (Exception e) {
                  throw new CastException("The connection attribute (" + connectionAttributeAsString + ") is not valid. We were expecting one of " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(ConnectionAttributeBase.class), e);
                }
                // Driver is a special attribute that stores the third party attribute
                if (connectionAttributeBase == ConnectionAttributeBase.DRIVER) {

                  Map<String, String> yamlDriverPropertiesMap = Casts.castToSameMap(confConnectionAttribute.getValue(), String.class, String.class);
                  for (Map.Entry<String, String> yamlDriverProperty : yamlDriverPropertiesMap.entrySet()) {
                    Variable driverVariable;
                    String driverConnectionAttribute = yamlDriverProperty.getKey();
                    try {
                      driverVariable = vault.createVariable(driverConnectionAttribute, yamlDriverProperty.getValue(), Origin.CONF);
                    } catch (Exception e) {
                      throw new RuntimeException("An error has occurred while reading the driver connection attribute " + driverConnectionAttribute + " value for the connection (" + connectionName + "). Error: " + e.getMessage(), e);
                    }
                    driverVariableMap.add(driverVariable);
                  }
                  continue;
                }

                Variable variable;
                try {
                  variable = vault.createVariable(connectionAttributeBase, confConnectionAttribute.getValue().toString(), Origin.CONF);
                } catch (Exception e) {
                  throw new RuntimeException("An error has occurred while reading the connection attribute " + connectionAttributeAsString + " value for the connection (" + connectionName + "). Error: " + e.getMessage(), e);
                }
                if (connectionAttributeBase == ConnectionAttributeBase.URI) {
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
              connection.addVariable(vault.createVariable(ConnectionAttributeBase.ORIGIN, ConnectionOrigin.CONF, Origin.RUNTIME));
              connection.setDriverVariables(driverVariableMap);
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


  public void flush() {
    flush(this.path);
  }

  public void flush(Path targetPath) {

    try {

      if (!Files.exists(targetPath)) {
        Fs.createEmptyFile(targetPath);
      }

      // Configure SnakeYAML settings
      DumperOptions dumperOptions = new DumperOptions();
      dumperOptions.setIndent(2);
      dumperOptions.setPrettyFlow(true);
      dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
      Yaml yaml = new Yaml(dumperOptions);
      Map<String, Object> confAsMap = new HashMap<>();
      confAsMap.put(KeyNormalizer.create(ConfVaultRootAttribute.CONNECTIONS).toCase(outputCase), toConnectionMap());
      confAsMap.put(KeyNormalizer.create(ConfVaultRootAttribute.VARIABLES).toCase(outputCase), toVariables());
      String yamlString = yaml.dump(confAsMap);
      // Write to file
      try (FileWriter writer = new FileWriter(targetPath.toFile())) {
        writer.write(yamlString);
      }

    } catch (IOException e) {

      throw new RuntimeException("Error while writing the configuration file " + targetPath.toAbsolutePath() + ". Error" + e.getMessage(), e);

    }
  }

  private Map<String, Object> toVariables() {

    Map<String, Object> variableMap = new HashMap<>();
    for (Variable variable : tabular.getVariables()) {
      dumpVariable(variable, variableMap);
    }
    return variableMap;
  }

  private Map<String, Object> toConnectionMap() {

    List<Connection> connections = new ArrayList<>(this.connections.values());
    Collections.sort(connections);
    Map<String, Object> connectionsMap = new HashMap<>();
    // For whatever reason, we made a name variable
    List<Attribute> noDumpAttributes = Arrays.asList(ConnectionAttributeBase.NAME, ConnectionAttributeBase.ORIGIN);
    for (Connection connection : connections) {
      String connectionNameSection = connection.toString();
      Map<String, Object> connectionMap = new HashMap<>();
      connectionsMap.put(connectionNameSection, connectionMap);

      for (Variable variable : connection.getVariables()) {
        if (noDumpAttributes.contains(variable.getAttribute())) {
          continue;
        }
        dumpVariable(variable, connectionMap);
      }

    }

    return connectionsMap;
  }

  private void dumpVariable(Variable variable, Map<String, Object> connectionMap) {
    // Derived value
    if (variable.isValueProvider()) {
      return;
    }
    String originalValue = variable.getCipherValue();
    if (originalValue == null || originalValue.isEmpty()) {
      return;
    }
    String key = KeyNormalizer.create(variable.getAttribute()).toCase(outputCase);
    connectionMap.put(key, originalValue);
  }

  /**
   * Used in Tabli
   */
  public ConfVault addVariable(String key, String value) throws CastException {
    TabularAttribute tabularAttribute;
    try {
      tabularAttribute = Casts.cast(key, TabularAttribute.class);
    } catch (CastException e) {
      throw new CastException("Error: the variable name (" + key + " is not a valid variable name. We were expecting one of: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TabularAttribute.class));
    }
    Variable variable = vault.createVariable(tabularAttribute, value, Origin.CONF);
    env.put(tabularAttribute, variable);
    return this;
  }

  @Override
  public String toString() {
    return path.toAbsolutePath().toString();
  }

  public Connection getConnection(String name) {
    return this.connections.get(name);
  }

  /**
   * Load the {@link com.tabulify.connection.ConnectionHowTos how-to connections}
   */
  public ConfVault loadHowtoConnections() {

    for (Connection connection : this.tabular.getHowtoConnections().values()) {
      addConnection(connection);
    }

    return this;

  }

  public ConfVault addConnection(Connection connection) {
    connections.put(connection.getName(), connection);
    return this;
  }

  public Object deleteVariable(String key) throws CastException {

    TabularAttribute tabularAttribute;
    try {
      tabularAttribute = Casts.cast(key, TabularAttribute.class);
    } catch (CastException e) {
      throw new CastException("Error: the variable name (" + key + " is not a valid variable name. We were expecting one of: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TabularAttribute.class));
    }
    Variable variable = env.remove(tabularAttribute);
    return variable.getCipherValue();

  }

  public Set<Variable> getVariables() {
    return new HashSet<>(this.env.values());
  }

}
