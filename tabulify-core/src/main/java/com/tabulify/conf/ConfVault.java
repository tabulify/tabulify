package com.tabulify.conf;

import com.tabulify.Tabular;
import com.tabulify.TabularAttributeEnum;
import com.tabulify.Vault;
import com.tabulify.connection.Connection;
import com.tabulify.connection.ConnectionAttributeEnum;
import com.tabulify.connection.ConnectionAttributeEnumBase;
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

  private final Map<TabularAttributeEnum, Attribute> global = new HashMap<>();
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

  public Attribute getAttribute(TabularAttributeEnum name) {
    return this.global.get(name);
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
          case GLOBALS:

            Map<String, String> localEnvs;
            try {
              localEnvs = Casts.castToSameMap(rootEntry.getValue(), String.class, String.class);
            } catch (CastException e) {
              throw new CastException("Error: " + e.getMessage() + ". " + badMapCast(data, String.valueOf(ConfVaultRootAttribute.GLOBALS)), e);
            }
            for (Map.Entry<String, String> localEnv : localEnvs.entrySet()) {

              TabularAttributeEnum tabularAttribute;
              String variableName = localEnv.getKey();
              try {
                tabularAttribute = Casts.cast(variableName, TabularAttributeEnum.class);
              } catch (ClassCastException e) {
                throw new CastException("The attribute parameter (" + variableName + ") is not valid. We were expecting one of " + tabular.toPublicListOfParameters(TabularAttributeEnum.class), e);
              }
              if (!tabularAttribute.isParameter()) {
                throw new CastException("The attribute (" + variableName + ") is not a parameter and cannot be modified.");
              }
              Attribute attribute = vault.createAttribute(tabularAttribute, localEnv.getValue(), Origin.CONF);
              global.put(tabularAttribute, attribute);

            }

            break;
          case CONNECTIONS:

            Map<String, Object> yamlConnections;
            try {
              yamlConnections = Casts.castToSameMap(rootEntry.getValue(), String.class, Object.class);
            } catch (CastException e) {
              throw new CastException("Error: " + e.getMessage() + ". " + badMapCast(data, String.valueOf(ConfVaultRootAttribute.CONNECTIONS)), e);
            }


            for (Map.Entry<String, Object> yamlConnection : yamlConnections.entrySet()) {

              String connectionName = yamlConnection.getKey();
              Map<KeyNormalizer, Object> yamlConnectionAttributes = Casts.castToNewMap(yamlConnection.getValue(), KeyNormalizer.class, Object.class);
              KeyNormalizer uriKeyNormalized = KeyNormalizer.create(ConnectionAttributeEnumBase.URI);
              String uri = (String) yamlConnectionAttributes.get(uriKeyNormalized);
              if (uri == null) {
                throw new RuntimeException("The uri is a mandatory variable and was not found for the connection (" + connectionName + ") in the conf file (" + this + ")");
              }

              /**
               * Create the connection
               */
              Connection connection = Connection.createConnectionFromProviderOrDefault(this.tabular, connectionName, uri)
                .addAttribute(vault.createAttribute(ConnectionAttributeEnumBase.ORIGIN, ConnectionOrigin.CONF, Origin.RUNTIME));

              /**
               * Native Attributes is a special attribute that stores the third party attribute
               */
              KeyNormalizer nativeAttributeNormalized = KeyNormalizer.create(ConnectionAttributeEnumBase.NATIVES);
              Object nativeAttributesAsObject = yamlConnectionAttributes.get(nativeAttributeNormalized);
              if (nativeAttributesAsObject != null) {

                Map<String, String> yamlDriverPropertiesMap = Casts.castToSameMap(nativeAttributesAsObject, String.class, String.class);
                for (Map.Entry<String, String> yamlDriverProperty : yamlDriverPropertiesMap.entrySet()) {
                  Attribute driverAttribute;
                  String nativeDriverPropertyName = yamlDriverProperty.getKey();
                  try {
                    driverAttribute = vault.createAttribute(nativeDriverPropertyName, yamlDriverProperty.getValue(), Origin.CONF);
                  } catch (Exception e) {
                    throw new RuntimeException("An error has occurred while reading the driver connection attribute " + nativeDriverPropertyName + " value for the connection (" + connectionName + "). Error: " + e.getMessage(), e);
                  }
                  connection.putNativeAttribute(nativeDriverPropertyName, driverAttribute);
                }
              }


              for (Map.Entry<KeyNormalizer, Object> confConnectionAttribute : yamlConnectionAttributes.entrySet()) {

                KeyNormalizer normalizedConnectionAttribute = confConnectionAttribute.getKey();

                // Already processed
                if (normalizedConnectionAttribute == uriKeyNormalized || normalizedConnectionAttribute == nativeAttributeNormalized) {
                  continue;
                }

                connection.addAttribute(normalizedConnectionAttribute, confConnectionAttribute.getValue(), Origin.CONF);

              }

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
      Map<String, Object> connectionMap = toConnectionMap();
      if (!connectionMap.isEmpty()) {
        confAsMap.put(KeyNormalizer.create(ConfVaultRootAttribute.CONNECTIONS).toCase(outputCase), connectionMap);
      }
      Map<String, Object> confParameters = toConfParameters();
      if (!confParameters.isEmpty()) {
        confAsMap.put(KeyNormalizer.create(ConfVaultRootAttribute.GLOBALS).toCase(outputCase), confParameters);
      }
      String yamlString = yaml.dump(confAsMap);
      // Write to file
      try (FileWriter writer = new FileWriter(targetPath.toFile())) {
        writer.write(yamlString);
      }

    } catch (IOException e) {

      throw new RuntimeException("Error while writing the configuration file " + targetPath.toAbsolutePath() + ". Error" + e.getMessage(), e);

    }
  }

  private Map<String, Object> toConfParameters() {

    Map<String, Object> variableMap = new HashMap<>();
    for (Attribute attribute : getParameters()) {
      String originalValue = attribute.getRawValue();
      String key = KeyNormalizer.create(attribute.getAttributeMetadata()).toCase(outputCase);
      variableMap.put(key, originalValue);
    }
    return variableMap;
  }

  private Map<String, Object> toConnectionMap() {

    List<Connection> connections = new ArrayList<>(this.connections.values());
    Collections.sort(connections);
    Map<String, Object> connectionsMap = new HashMap<>();
    // For whatever reason, we made a name variable
    List<AttributeEnum> noDumpAttributes = Arrays.asList(ConnectionAttributeEnumBase.NAME, ConnectionAttributeEnumBase.ORIGIN);
    for (Connection connection : connections) {
      String connectionNameSection = connection.toString();
      Map<String, Object> connectionMap = new HashMap<>();
      connectionsMap.put(connectionNameSection, connectionMap);

      for (Attribute attribute : connection.getAttributes()) {
        ConnectionAttributeEnum attributeEnum = (ConnectionAttributeEnum) attribute.getAttributeMetadata();
        if (noDumpAttributes.contains(attributeEnum)) {
          continue;
        }
        if (!attributeEnum.isParameter()) {
          continue;
        }
        String key = KeyNormalizer.create(attributeEnum).toCase(outputCase);
        if (attributeEnum.equals(ConnectionAttributeEnumBase.NATIVES)) {
          Map<String, String> nativeDriverAttributes = connection.getNativeDriverAttributes();
          if (nativeDriverAttributes.isEmpty()) {
            continue;
          }
          connectionMap.put(key, nativeDriverAttributes);
          continue;
        }
        String originalValue = attribute.getRawValue();
        if (originalValue == null || originalValue.isEmpty()) {
          continue;
        }
        connectionMap.put(key, originalValue);
      }

    }

    return connectionsMap;
  }


  /**
   * Used in Tabli
   */
  public ConfVault addAttribute(String key, String value) throws CastException {
    TabularAttributeEnum tabularAttribute;
    try {
      tabularAttribute = Casts.cast(key, TabularAttributeEnum.class);
    } catch (CastException e) {
      throw new CastException("Error: the variable name (" + key + " is not a valid variable name. We were expecting one of: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TabularAttributeEnum.class));
    }
    Attribute attribute = vault.createAttribute(tabularAttribute, value, Origin.CONF);
    global.put(tabularAttribute, attribute);
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

    TabularAttributeEnum tabularAttribute;
    try {
      tabularAttribute = Casts.cast(key, TabularAttributeEnum.class);
    } catch (CastException e) {
      throw new CastException("Error: the variable name (" + key + " is not a valid variable name. We were expecting one of: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TabularAttributeEnum.class));
    }
    Attribute attribute = global.remove(tabularAttribute);
    return attribute.getRawValue();

  }

  public Set<Attribute> getParameters() {
    return new HashSet<>(this.global.values());
  }


}
