package com.tabulify.conf;

import com.tabulify.Tabular;
import com.tabulify.TabularAttributeEnum;
import com.tabulify.Vault;
import com.tabulify.connection.Connection;
import com.tabulify.connection.ConnectionAttributeEnum;
import com.tabulify.connection.ConnectionAttributeEnumBase;
import com.tabulify.connection.ObjectOrigin;
import com.tabulify.howto.Howtos;
import com.tabulify.service.Service;
import com.tabulify.service.ServiceAttributeBase;
import com.tabulify.service.ServiceAttributeEnum;
import com.tabulify.service.Services;
import net.bytle.exception.CastException;
import net.bytle.exception.ExceptionWrapper;
import net.bytle.fs.Fs;
import net.bytle.regexp.Glob;
import net.bytle.type.*;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.representer.Representer;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A class that manages a configuration file
 */
public class ConfVault {


  public static final KeyNormalizer TABUL = KeyNormalizer.createSafe("tabul");
  private final Vault vault;
  private final Path path;

  private final Map<TabularAttributeEnum, Attribute> env = new HashMap<>();
  private final Tabular tabular;
  private final Map<KeyNormalizer, Connection> connections = new HashMap<>();
  private final Map<KeyNormalizer, Service> services = new HashMap<>();
  private final KeyCase outputCase = KeyCase.HYPHEN;

  /**
   * @param path    - the file
   * @param tabular - tabular for the vault and to create the connections
   *                All parameters are there to force the initialization order.
   *                ie Vault is reachable from the tabular global object,
   *                but it should be created before confManager
   */
  private ConfVault(Path path, Tabular tabular) {

    this.tabular = tabular;
    this.vault = tabular.getVault();
    this.path = path;

    if (path == null) {
      // When we don't want to load it for testing purpose
      // path may be null
      return;
    }


    List<String> yamlExtensions = Arrays.asList("yml", "yaml");
    if (!yamlExtensions.contains(Fs.getExtension(path))) {

      throw new IllegalArgumentException("The configuration file (" + path + ") should be a yaml file with the extension (" + String.join(", ", yamlExtensions) + " )");

    }

    try {
      parseYaml();
    } catch (Exception e) {
      // we cannot recover from that
      throw ExceptionWrapper.builder(e, "Error while parsing the config vault " + this + ".")
        .setPosition(ExceptionWrapper.ContextPosition.FIRST)
        .buildAsRuntimeException();
    }

  }


  public static ConfVault createFromPath(Path confPath, Tabular tabular) {

    return new ConfVault(confPath, tabular);
  }

  public static ConfVault createEmpty(Tabular tabular) {
    return new ConfVault(null, tabular);
  }

  public Attribute getAttribute(TabularAttributeEnum name) {
    return this.env.get(name);
  }


  /**
   * Parse the {@link #path} if the file exists and is not null
   */
  private void parseYaml() throws CastException {


    if (!Files.exists(path)) {
      return;
    }

    long size;
    try {
      size = Files.size(path);
    } catch (IOException e) {
      // error when reading size, almost never happen
      size = 0;
    }
    if (size == 0) {
      return;
    }

    ManifestDocument manifestDocument = ManifestDocument.builder()
      .setPath(path)
      .build();

    if (!manifestDocument.getKind().equals(TABUL)) {
      throw new IllegalArgumentException("The manifest " + manifestDocument + " is not a " + TABUL + " manifest but a " + manifestDocument.getKind());
    }

    for (Map.Entry<KeyNormalizer, Object> rootEntry : manifestDocument.getSpecMap().entrySet()) {

      KeyNormalizer rootName = rootEntry.getKey();
      ConfVaultRootAttribute rootAttribute;
      try {
        rootAttribute = Casts.cast(rootName, ConfVaultRootAttribute.class);
      } catch (CastException e) {
        throw new CastException("The root name (" + rootName + ") is not valid. We were expecting one of " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(ConfVaultRootAttribute.class), e);
      }

      Object value = rootEntry.getValue();
      switch (rootAttribute) {
        case ENVS:

          Map<String, String> localEnvs;
          try {
            localEnvs = Casts.castToSameMap(value, String.class, String.class);
          } catch (CastException e) {
            throw new CastException("Error: " + e.getMessage() + ". " + badMapCast(value, String.valueOf(ConfVaultRootAttribute.ENVS)), e);
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
            Attribute attribute = vault.createAttribute(tabularAttribute, localEnv.getValue(), Origin.MANIFEST);
            env.put(tabularAttribute, attribute);

          }

          break;
        case CONNECTIONS:

          Map<String, Object> yamlConnections;
          try {
            yamlConnections = Casts.castToSameMap(value, String.class, Object.class);
          } catch (CastException e) {
            throw new CastException("Error: " + e.getMessage() + ". " + badMapCast(value, String.valueOf(ConfVaultRootAttribute.CONNECTIONS)), e);
          }

          for (Map.Entry<String, Object> yamlConnection : yamlConnections.entrySet()) {

            KeyNormalizer connectionName;
            String connectionNameAsString = yamlConnection.getKey();
            try {
              connectionName = KeyNormalizer.create(connectionNameAsString);
            } catch (CastException e) {
              throw ExceptionWrapper.builder(e, "The connection name (" + connectionNameAsString + ") is not valid. Error: " + e.getMessage())
                .setPosition(ExceptionWrapper.ContextPosition.FIRST)
                .buildAsRuntimeException();
            }
            Map<KeyNormalizer, Object> yamlConnectionAttributes = Casts.castToNewMap(yamlConnection.getValue(), KeyNormalizer.class, Object.class);
            KeyNormalizer uriKeyNormalized = KeyNormalizer.createSafe(ConnectionAttributeEnumBase.URI);
            String uri = (String) yamlConnectionAttributes.get(uriKeyNormalized);
            if (uri == null) {
              throw new RuntimeException("The uri is a attribute variable and was not found for the connection (" + connectionName + ") in the conf file (" + this + ")");
            }

            /**
             * Create the connection
             */
            Connection connection = Connection.createConnectionFromProviderOrDefault(this.tabular, connectionName, uri)
              .addAttribute(vault.createAttribute(ConnectionAttributeEnumBase.ORIGIN, ObjectOrigin.CONF, Origin.DEFAULT));

            /**
             * Native Attributes is a special attribute that stores the third party attribute
             */
            KeyNormalizer nativeAttributeNormalized = KeyNormalizer.createSafe(ConnectionAttributeEnumBase.NATIVES);
            Object nativeAttributesAsObject = yamlConnectionAttributes.get(nativeAttributeNormalized);
            if (nativeAttributesAsObject != null) {

              Map<String, String> yamlDriverPropertiesMap = Casts.castToSameMap(nativeAttributesAsObject, String.class, String.class);
              for (Map.Entry<String, String> yamlDriverProperty : yamlDriverPropertiesMap.entrySet()) {
                String nativeDriverPropertyName = yamlDriverProperty.getKey();
                connection.addNativeAttribute(nativeDriverPropertyName, yamlDriverProperty.getValue(), Origin.MANIFEST, tabular.getVault());
              }
            }

            /**
             * Tabul Attributes
             */
            for (Map.Entry<KeyNormalizer, Object> confConnectionAttribute : yamlConnectionAttributes.entrySet()) {

              KeyNormalizer normalizedConnectionAttribute = confConnectionAttribute.getKey();

              // Already processed
              if (normalizedConnectionAttribute.equals(uriKeyNormalized) || normalizedConnectionAttribute.equals(nativeAttributeNormalized)) {
                continue;
              }

              Object valueConfConnection = confConnectionAttribute.getValue();
              connection.addAttribute(normalizedConnectionAttribute, valueConfConnection, Origin.MANIFEST);

            }

            connections.put(connectionName, connection);

          }

          break;
        case SERVICES:

          Map<String, Object> yamlSystems;
          try {
            yamlSystems = Casts.castToSameMap(value, String.class, Object.class);
          } catch (CastException e) {
            throw new CastException("Error: " + e.getMessage() + ". " + badMapCast(value, String.valueOf(ConfVaultRootAttribute.SERVICES)), e);
          }


          for (Map.Entry<String, Object> yamlConnection : yamlSystems.entrySet()) {

            KeyNormalizer serviceName = null;
            String serviceNameAsString = yamlConnection.getKey();
            try {
              serviceName = KeyNormalizer.create(serviceNameAsString);
            } catch (CastException e) {
              throw new IllegalArgumentException("The service name (" + serviceNameAsString + ") is not valid. Error: " + e.getMessage(), e);
            }
            Map<KeyNormalizer, Object> yamlConnectionAttributes = Casts.castToNewMap(yamlConnection.getValue(), KeyNormalizer.class, Object.class);
            KeyNormalizer typeNormalized = KeyNormalizer.createSafe(ServiceAttributeBase.TYPE);
            String type = (String) yamlConnectionAttributes.get(typeNormalized);
            if (type == null) {
              throw new IllegalArgumentException("The " + ServiceAttributeBase.TYPE + " is a mandatory attribute and was not found for the service (" + serviceName + ")");
            }

            /**
             * Create the connection
             */
            Attribute serviceNameAttribute = vault.createAttribute(ServiceAttributeBase.NAME, serviceName, Origin.MANIFEST);
            Attribute serviceTypeAttribute = vault.createAttribute(ServiceAttributeBase.NAME, type, Origin.MANIFEST);
            Service systemConnection = Services.createService(this.tabular, serviceNameAttribute, serviceTypeAttribute)
              .addAttribute(ServiceAttributeBase.ORIGIN, Origin.DEFAULT, ObjectOrigin.CONF);

            /**
             * Tabli Attributes
             */
            for (Map.Entry<KeyNormalizer, Object> confConnectionAttribute : yamlConnectionAttributes.entrySet()) {

              KeyNormalizer normalizedConnectionAttribute = confConnectionAttribute.getKey();

              // Already processed
              if (normalizedConnectionAttribute == typeNormalized) {
                continue;
              }

              systemConnection.addAttribute(normalizedConnectionAttribute, Origin.MANIFEST, confConnectionAttribute.getValue());

            }

            services.put(serviceName, systemConnection);

          }

          break;
        default:
          throw new RuntimeException("The root attribute " + rootAttribute + " was not in the switch");
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


  public ConfVault flush() {
    flush(this.path);
    return this;
  }

  public void flush(Path targetPath) {

    try {

      if (!Files.exists(targetPath)) {
        Fs.createEmptyFile(targetPath);
      }


      Yaml yaml = createYaml();

      Map<String, Object> manifestAsMap = new HashMap<>();
      manifestAsMap.put(ManifestAttribute.KIND.toString().toLowerCase(), TABUL.toKebabCase());
      Map<String, Object> confAsMap = new HashMap<>();
      manifestAsMap.put(ManifestAttribute.SPEC.toString().toLowerCase(), confAsMap);
      Map<String, Object> confParameters = toConfParameters();
      // Seems that the hash map uses a reverse order
      // services last, connection second and env first
      Map<String, Object> serviceMap = toServiceMapForDump();
      if (!serviceMap.isEmpty()) {
        confAsMap.put(KeyNormalizer.createSafe(ConfVaultRootAttribute.SERVICES).toCase(outputCase), serviceMap);
      }
      Map<String, Object> connectionMap = toConnectionMapForDump();
      if (!connectionMap.isEmpty()) {
        confAsMap.put(KeyNormalizer.createSafe(ConfVaultRootAttribute.CONNECTIONS).toCase(outputCase), connectionMap);
      }
      if (!confParameters.isEmpty()) {
        confAsMap.put(KeyNormalizer.createSafe(ConfVaultRootAttribute.ENVS).toCase(outputCase), confParameters);
      }

      String yamlString = yaml.dump(manifestAsMap);
      // Write to file
      try (FileWriter writer = new FileWriter(targetPath.toFile())) {
        writer.write(yamlString);
      }

    } catch (IOException e) {

      throw new RuntimeException("Error while writing the configuration file " + targetPath.toAbsolutePath() + ". Error" + e.getMessage(), e);

    }
  }

  /**
   *
   */
  private static Yaml createYaml() {

    /**
     * Options
     */
    DumperOptions dumperOptions = new DumperOptions();
    dumperOptions.setIndent(2);
    dumperOptions.setPrettyFlow(true);
    dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

    /**
     * A {@link Representer} to write attributes in order
     */
    Representer representer = new Representer(dumperOptions) {

      @Override
      protected Node representMapping(Tag tag, Map<?, ?> mapping, DumperOptions.FlowStyle flowStyle) {
        MappingNode node = (MappingNode) super.representMapping(tag, mapping, flowStyle);
        List<NodeTuple> tuples = node.getValue();

        // Sort the node tuples based on our custom order
        tuples.sort((a, b) -> {
          if (a.getKeyNode() instanceof ScalarNode && b.getKeyNode() instanceof ScalarNode) {
            String keyA = ((ScalarNode) a.getKeyNode()).getValue();
            String keyB = ((ScalarNode) b.getKeyNode()).getValue();
            try {
              return getOrderTopOrder(keyA) - getOrderTopOrder(keyB);
            } catch (CastException e) {
              return keyA.compareTo(keyB);
            }
          }
          return 0;
        });

        return node;
      }


      private int getOrderTopOrder(String propertyName) throws CastException {

        return Casts.cast(propertyName, ConfVaultRootAttribute.class).getOrder();

      }


    };
    return new Yaml(representer, dumperOptions);
  }

  private Map<String, Object> toServiceMapForDump() {
    List<Service> services = new ArrayList<>(this.services.values());
    Collections.sort(services);
    Map<String, Object> servicesMap = new HashMap<>();
    // name is a variable so that we have the origin has every attribute
    List<AttributeEnum> noDumpAttributes = Arrays.asList(ServiceAttributeBase.NAME, ServiceAttributeBase.ORIGIN);
    for (Service service : services) {
      KeyNormalizer serviceName = service.getName();
      Map<String, Object> serviceMap = new HashMap<>();
      servicesMap.put(serviceName.toString(), serviceMap);

      for (Attribute attribute : service.getAttributes()) {
        ServiceAttributeEnum attributeEnum = (ServiceAttributeEnum) attribute.getAttributeMetadata();
        if (noDumpAttributes.contains(attributeEnum)) {
          continue;
        }
        if (!attributeEnum.isParameter()) {
          continue;
        }
        String key = KeyNormalizer.createSafe(attributeEnum).toCase(outputCase);
        Object originalValue = attribute.getRawValue();
        if (originalValue == null) {
          continue;
        }
        if (originalValue instanceof Map) {
          if (((Map<?, ?>) originalValue).isEmpty()) {
            continue;
          }
          serviceMap.put(key, originalValue);
          continue;
        }
        if (originalValue instanceof Collection) {
          if (((Collection<?>) originalValue).isEmpty()) {
            continue;
          }
          serviceMap.put(key, originalValue);
          continue;
        }
        String originalStringValue = originalValue.toString();
        if (originalStringValue.isEmpty()) {
          continue;
        }
        serviceMap.put(key, originalStringValue);
      }

    }

    return servicesMap;
  }

  private Map<String, Object> toConfParameters() {

    Map<String, Object> variableMap = new HashMap<>();
    for (Attribute attribute : getParameters()) {
      String originalValue = (String) attribute.getRawValue();
      String key = KeyNormalizer.createSafe(attribute.getAttributeMetadata()).toCase(outputCase);
      variableMap.put(key, originalValue);
    }
    return variableMap;
  }

  /**
   * A map for a dump
   */
  private Map<String, Object> toConnectionMapForDump() {

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
        String key = KeyNormalizer.createSafe(attributeEnum).toCase(outputCase);
        if (attributeEnum.equals(ConnectionAttributeEnumBase.NATIVES)) {
          Map<String, String> nativeDriverAttributes = connection.getNativeDriverAttributes()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
              Map.Entry::getKey,
              e -> e.getValue().getValueOrDefaultAsStringNotNull()
            ));
          if (nativeDriverAttributes.isEmpty()) {
            continue;
          }
          connectionMap.put(key, nativeDriverAttributes);
          continue;
        }

        Object originalValue = attribute.getRawValue();
        if (originalValue == null || ObjectStatics.isEmpty(originalValue)) {
          continue;
        }
        Origin origin = attribute.getOrigin();
        if (!origin.isInVaultConf()) {
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
    Attribute attribute = vault.createAttribute(tabularAttribute, value, Origin.MANIFEST);
    env.put(tabularAttribute, attribute);
    return this;
  }

  @Override
  public String toString() {
    return path.toAbsolutePath().toString();
  }

  public Connection getConnection(KeyNormalizer name) {
    return this.connections.get(name);
  }

  /**
   * Load the connections
   */
  public ConfVault loadHowtoConnections() {


    Set<Connection> connections = Howtos.getConnections(tabular);
    for (Connection connection : connections) {
      addConnection(connection);
    }

    return this;

  }

  public ConfVault addConnection(Connection connection) {
    connections.put(connection.getName(), connection);
    return this;
  }

  public Set<Attribute> deleteAttributesByGlobName(String globName) {

    Set<TabularAttributeEnum> tabularAttributeEnums = env.keySet()
      .stream()
      .filter(attributeEnum -> Glob.createOf(globName).matches(attributeEnum.toString().toLowerCase()))
      .collect(Collectors.toSet());

    Set<Attribute> attributesDeleted = new HashSet<>();
    for (TabularAttributeEnum tabularAttribute : tabularAttributeEnums) {
      attributesDeleted.add(env.remove(tabularAttribute));
    }
    return attributesDeleted;

  }

  public Set<Attribute> getParameters() {
    return new HashSet<>(this.env.values());
  }


  /**
   * @param globName - a glob
   * @return the deleted connection name
   */
  public Set<KeyNormalizer> deleteConnectionByGlobName(String globName) {

    Set<KeyNormalizer> connectNamesToDelete = connections.keySet()
      .stream()
      .filter(connectionName -> Glob.createOf(globName).matches(connectionName.toString()))
      .collect(Collectors.toSet());
    for (KeyNormalizer name : connectNamesToDelete) {
      connections
        .remove(name)
        .close();
    }
    return connectNamesToDelete;

  }


  public HashSet<Connection> getConnections() {
    return new HashSet<>(connections.values());
  }

  public ConfVault loadHowtoServices() {
    Set<Service> services = Howtos.getServices(tabular);
    for (Service service : services) {
      addService(service);
    }
    return this;
  }

  private void addService(Service service) {

    services.put(service.getName(), service);
  }

  public Map<KeyNormalizer, Service> getServices() {
    return services;
  }


  public Path getPath() {
    return this.path;
  }

}
