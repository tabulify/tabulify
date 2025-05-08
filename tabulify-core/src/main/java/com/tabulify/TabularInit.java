package com.tabulify;

import com.tabulify.conf.*;
import com.tabulify.connection.Connection;
import com.tabulify.connection.ConnectionHowTos;
import net.bytle.exception.CastException;
import net.bytle.fs.Fs;
import net.bytle.java.JavaEnvs;
import net.bytle.java.Javas;
import net.bytle.type.Casts;
import net.bytle.type.Enums;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MapKeyIndependent;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tabulify.DbLoggers.LOGGER_TABULAR_START;
import static com.tabulify.Tabular.TABLI_NAME;

/**
 * Just a class to store all init procedures
 * so that the tabular object is uncluttered
 */
public class TabularInit {


  static TabularExecEnv determineEnv(TabularExecEnv env, Vault vault, TabularEnvs tabularEnvs, Map<TabularAttributeEnum, com.tabulify.conf.Attribute> attributeMap, ConfVault confVault) {

    TabularAttributeEnum attribute = TabularAttributeEnum.ENV;
    Vault.VariableBuilder configVariable = vault.createVariableBuilderFromAttribute(attribute);
    TabularExecEnv value;

    // Env
    if (env != null) {
      LOGGER_TABULAR_START.info("Tabli env: Passed as argument " + env);
      com.tabulify.conf.Attribute variable = configVariable
        .setOrigin(com.tabulify.conf.Origin.COMMAND_LINE)
        .buildSafe(env.toString());
      attributeMap.put((TabularAttributeEnum) variable.getAttributeMetadata(), variable);
      return env;
    }

    /**
     * Conf Manager
     */
    com.tabulify.conf.Attribute confAttribute = confVault.getAttribute(attribute);
    if (confAttribute != null) {
      String confEnvValue = confAttribute.getValueOrDefaultAsStringNotNull();
      try {
        value = Casts.cast(confEnvValue, TabularExecEnv.class);
      } catch (CastException e) {
        throw new RuntimeException("The env value (" + confEnvValue + ") in the conf file is not correct. We were expecting one of: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TabularExecEnv.class), e);
      }
      attributeMap.put((TabularAttributeEnum) confAttribute.getAttributeMetadata(), confAttribute);
      return value;
    }

    /**
     * Sys
     */
    KeyNormalizer osEnvName = tabularEnvs.getNormalizedKey(attribute);
    String javaSysValue = tabularEnvs.getJavaSysValue(osEnvName);
    if (javaSysValue != null) {
      try {
        LOGGER_TABULAR_START.info("Tabli env: Found in Java Sys env " + osEnvName + " with the value " + javaSysValue);
        value = Casts.cast(javaSysValue, TabularExecEnv.class);
        com.tabulify.conf.Attribute variable = configVariable
          .setOrigin(Origin.SYS)
          .build(value.toString());
        attributeMap.put((TabularAttributeEnum) variable.getAttributeMetadata(), variable);
        return value;
      } catch (CastException e) {
        throw new IllegalArgumentException("The java sys (" + osEnvName + ") has a value (" + javaSysValue + ") that is unknown. Possible values: " + Enums.toConstantAsStringCommaSeparated(TabularExecEnv.class), e);
      }
    }

    /**
     * Os
     */
    String envOsValue = tabularEnvs.getOsEnvValue(osEnvName);
    if (envOsValue != null) {
      try {
        LOGGER_TABULAR_START.info("Tabli env: Found in OS env " + osEnvName.toEnvName() + " with the value " + envOsValue);
        value = Casts.cast(envOsValue, TabularExecEnv.class);
        com.tabulify.conf.Attribute variable = configVariable
          .setOrigin(com.tabulify.conf.Origin.OS)
          .build(value.toString());
        attributeMap.put((TabularAttributeEnum) variable.getAttributeMetadata(), variable);
        return value;
      } catch (CastException e) {
        throw new IllegalArgumentException("The os env (" + osEnvName.toEnvName() + ") has a env value (" + envOsValue + ") that is unknown. Possible values: " + Enums.toConstantAsStringCommaSeparated(TabularExecEnv.class), e);
      }
    }

    if (JavaEnvs.isJUnitTest()) {
      LOGGER_TABULAR_START.info("Tabli env: IDE as it's a junit run");
      value = TabularExecEnv.IDE;
      com.tabulify.conf.Attribute variable = configVariable
        .setOrigin(com.tabulify.conf.Origin.DEFAULT)
        .buildSafe(value);
      attributeMap.put((TabularAttributeEnum) variable.getAttributeMetadata(), variable);
      return value;
    }

    LOGGER_TABULAR_START.info("Tabli env: No value found, defaulted to dev");
    value = TabularExecEnv.DEV;
    com.tabulify.conf.Attribute variable = configVariable
      .setOrigin(com.tabulify.conf.Origin.DEFAULT)
      .buildSafe(value);
    attributeMap.put((TabularAttributeEnum) variable.getAttributeMetadata(), variable);
    return value;

  }


  /**
   * @param homePath the home path from the constructor
   */
  static Path determineHomePath(Path homePath, TabularExecEnv execEnv, TabularEnvs tabularEnvs, Map<TabularAttributeEnum, com.tabulify.conf.Attribute> attributeMap, Vault vault, ConfVault confVault) {

    TabularAttributeEnum attribute = TabularAttributeEnum.HOME;
    Vault.VariableBuilder variableBuilder = vault.createVariableBuilderFromAttribute(attribute);

    if (homePath != null) {
      com.tabulify.conf.Attribute variable = variableBuilder
        .setOrigin(com.tabulify.conf.Origin.COMMAND_LINE)
        .buildSafe(homePath);
      attributeMap.put((TabularAttributeEnum) variable.getAttributeMetadata(), variable);
      return homePath;
    }

    /**
     * Conf Manager
     */
    com.tabulify.conf.Attribute confHomeAttribute = confVault.getAttribute(attribute);
    if (confHomeAttribute != null) {
      String confEnvValue = confHomeAttribute.getValueOrDefaultAsStringNotNull();
      attributeMap.put((TabularAttributeEnum) confHomeAttribute.getAttributeMetadata(), confHomeAttribute);
      return Paths.get(confEnvValue);
    }

    /**
     * Sys
     */
    KeyNormalizer envName = tabularEnvs.getNormalizedKey(TabularAttributeEnum.HOME);
    String sysTabliHome = tabularEnvs.getJavaSysValue(envName);
    if (sysTabliHome != null) {
      com.tabulify.conf.Attribute variable = variableBuilder
        .setOrigin(Origin.SYS)
        .buildSafe(sysTabliHome);
      attributeMap.put((TabularAttributeEnum) variable.getAttributeMetadata(), variable);
      return Paths.get(sysTabliHome);
    }

    /**
     * Env
     */
    String tabliHome = tabularEnvs.getOsEnvValue(envName);
    if (tabliHome != null) {
      com.tabulify.conf.Attribute variable = variableBuilder
        .setOrigin(com.tabulify.conf.Origin.OS)
        .buildSafe(tabliHome);
      attributeMap.put((TabularAttributeEnum) variable.getAttributeMetadata(), variable);
      return Paths.get(tabliHome);
    }

    /**
     * In ide dev
     */
    if (execEnv == TabularExecEnv.IDE) {

      // in dev, we try to find the directory until the git directory is found
      // with Idea, the class are in the build directory,
      // but with maven, they are in the jars
      // We can't check the location of the class
      try {
        Path closestHomePath = Fs.closest(Paths.get("."), ".git").getParent();
        com.tabulify.conf.Attribute variable = variableBuilder
          .setOrigin(com.tabulify.conf.Origin.DEFAULT)
          .buildSafe(closestHomePath);
        attributeMap.put((TabularAttributeEnum) variable.getAttributeMetadata(), variable);
        return closestHomePath;
      } catch (FileNotFoundException e) {
        // Not found
      }

    }

    // In prod, the classes are in the jars directory
    // First getParent get the jars directory, getParent get the Home
    Path prodHomePath = Javas.getSourceCodePath(ConnectionHowTos.class).getParent().getParent();
    com.tabulify.conf.Attribute variable = variableBuilder
      .setOrigin(com.tabulify.conf.Origin.DEFAULT)
      .buildSafe(prodHomePath);
    attributeMap.put((TabularAttributeEnum) variable.getAttributeMetadata(), variable);
    return prodHomePath;

  }


  static public Path determineProjectHome(Path projectHomePath, Vault vault, Map<TabularAttributeEnum, com.tabulify.conf.Attribute> attributeMap, TabularEnvs tabularEnvs) {

    TabularAttributeEnum attributeEnum = TabularAttributeEnum.PROJECT_HOME;
    Vault.VariableBuilder confVariable = vault.createVariableBuilderFromAttribute(attributeEnum);

    if (projectHomePath != null) {
      com.tabulify.conf.Attribute attribute = confVariable
        .setOrigin(com.tabulify.conf.Origin.COMMAND_LINE)
        .buildSafe(projectHomePath);
      attributeMap.put((TabularAttributeEnum) attribute.getAttributeMetadata(), attribute);
      return Paths.get(attribute.getValueOrDefaultAsStringNotNull());
    }


    KeyNormalizer envName = tabularEnvs.getNormalizedKey(attributeEnum);
    // Sys
    String sysProjectHomeFromEnv = tabularEnvs.getJavaSysValue(envName);
    if (sysProjectHomeFromEnv != null) {
      com.tabulify.conf.Attribute attribute = confVariable
        .setOrigin(Origin.SYS)
        .buildSafe(sysProjectHomeFromEnv);
      attributeMap.put((TabularAttributeEnum) attribute.getAttributeMetadata(), attribute);
      return Paths.get(attribute.getValueOrDefaultAsStringNotNull());
    }

    // Env
    String projectHomeFromEnv = tabularEnvs.getOsEnvValue(envName);
    if (projectHomeFromEnv != null) {
      com.tabulify.conf.Attribute attribute = confVariable
        .setOrigin(com.tabulify.conf.Origin.OS)
        .buildSafe(projectHomeFromEnv);
      attributeMap.put((TabularAttributeEnum) attribute.getAttributeMetadata(), attribute);
      return Paths.get(attribute.getValueOrDefaultAsStringNotNull());
    }

    // Derived
    confVariable.setOrigin(com.tabulify.conf.Origin.DEFAULT);
    try {
      Path closestProjectHomePath = Fs.closest(Paths.get("."), Tabular.TABLI_CONF_FILE_NAME).getParent();
      if (closestProjectHomePath == null) {
        // to please the linter as getParent may return null ...
        throw new FileNotFoundException();
      }
      com.tabulify.conf.Attribute variable = confVariable.buildSafe(closestProjectHomePath.toString());
      attributeMap.put((TabularAttributeEnum) variable.getAttributeMetadata(), variable);
      return closestProjectHomePath;
    } catch (FileNotFoundException e) {
      // not a project
      com.tabulify.conf.Attribute variable = confVariable.buildSafe(null);
      attributeMap.put((TabularAttributeEnum) variable.getAttributeMetadata(), variable);
      return null;
    }
  }

  static public Path determineConfPath(Path confPath, Vault vault, TabularEnvs tabularEnvs, Path projectHome, Path userHome, Map<TabularAttributeEnum, com.tabulify.conf.Attribute> attributeMap) {

    Vault.VariableBuilder confVariable = vault.createVariableBuilderFromAttribute(TabularAttributeEnum.CONF);
    if (confPath != null) {
      Attribute attribute = confVariable
        .setOrigin(com.tabulify.conf.Origin.COMMAND_LINE)
        .buildSafe(confPath);
      attributeMap.put((TabularAttributeEnum) attribute.getAttributeMetadata(), attribute);
      return (Path) attribute.getValueOrDefaultOrNull();
    }

    KeyNormalizer osEnvName = tabularEnvs.getNormalizedKey(TabularAttributeEnum.CONF);
    // sys
    String sysConfPathString = tabularEnvs.getJavaSysValue(osEnvName);
    if (sysConfPathString != null) {
      Attribute attribute = confVariable
        .setOrigin(Origin.SYS)
        .buildSafe(sysConfPathString);
      attributeMap.put((TabularAttributeEnum) attribute.getAttributeMetadata(), attribute);
      return (Path) attribute.getValueOrDefaultOrNull();
    }

    // env
    String confPathString = tabularEnvs.getOsEnvValue(osEnvName);
    if (confPathString != null) {
      com.tabulify.conf.Attribute attribute = confVariable
        .setOrigin(com.tabulify.conf.Origin.OS)
        .buildSafe(confPathString);
      attributeMap.put((TabularAttributeEnum) attribute.getAttributeMetadata(), attribute);
      return (Path) attribute.getValueOrDefaultOrNull();
    }

    if (projectHome != null) {
      Path conf = projectHome.resolve(Tabular.TABLI_CONF_FILE_NAME);
      com.tabulify.conf.Attribute attribute = confVariable
        .setOrigin(com.tabulify.conf.Origin.DEFAULT)
        .buildSafe(conf);
      attributeMap.put((TabularAttributeEnum) attribute.getAttributeMetadata(), attribute);
      return (Path) attribute.getValueOrDefaultOrNull();
    }

    Path conf = userHome.resolve(Tabular.TABLI_CONF_FILE_NAME);
    com.tabulify.conf.Attribute attribute = confVariable
      .setOrigin(Origin.DEFAULT)
      .buildSafe(conf);
    attributeMap.put((TabularAttributeEnum) attribute.getAttributeMetadata(), attribute);
    return (Path) attribute.getValueOrDefaultOrNull();

  }


  /**
   * Loop over all env and check that attributes were created
   * Not really needed, but it helps with a bad typo
   */
  public static void checkForEnvNotProcessed(TabularEnvs tabularEnvs, Map<TabularAttributeEnum, Attribute> attributeMap, MapKeyIndependent<Connection> connections, Tabular tabular) {
    for (Map.Entry<String, String> tabularEnv : tabularEnvs.getEnvs().entrySet()) {
      String key = tabularEnv.getKey();
      String lowerCaseKey = key.toLowerCase();
      if (!lowerCaseKey.startsWith(TABLI_NAME)) {
        continue;
      }
      if (lowerCaseKey.equals(Tabular.TABLI_OS_USER_HOME.toLowerCase())) {
        // An internal hack to get the same os user home
        // when creating the documentation
        continue;
      }
      List<String> envValueParts;
      try {
        envValueParts = KeyNormalizer.create(lowerCaseKey).getParts();
      } catch (CastException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
      TabularAttributeEnum tabularAttributes;
      try {
        String keyWithoutTabli = envValueParts.stream().skip(1).collect(Collectors.joining("_"));
        tabularAttributes = Casts.cast(keyWithoutTabli, TabularAttributeEnum.class);
        com.tabulify.conf.Attribute attribute = attributeMap.get(tabularAttributes);
        if (attribute == null) {
          throw new RuntimeException("Internal error: The tabulify attribute (" + tabularAttributes + ") was not initialized");
        }
      } catch (CastException e) {
        // not a global/Tabular attribute, a connection one?
        if (envValueParts.size() < 3) {
          throw new RuntimeException("The environment variable (" + key + ") is unknown");
        }
        // Get the connection name
        String connectionName = "";
        Connection connection = null;
        for (int i = 1; i < envValueParts.size(); i++) {
          connectionName = String.join("_", envValueParts.subList(1, i + 1));
          connection = connections.get(connectionName);
          if (connection != null) {
            break;
          }
        }
        if (connection == null) {
          throw new RuntimeException("The environment variable (" + key + ") is unknown. It's not a global or connection attribute environment variable.");
        }
        String keyWithoutTabliAndConnection = envValueParts.stream().skip(2).collect(Collectors.joining("_"));
        List<Class<? extends AttributeEnumParameter>> attributeEnums = connection.getAttributeEnums();
        boolean found = false;
        for (Class<?> clazz : attributeEnums) {
          try {
            Casts.cast(keyWithoutTabliAndConnection, clazz);
            found = true;
            break;
          } catch (CastException ex) {
            // not for this clazz
          }
        }
        if (found) {
          continue;
        }
        throw new RuntimeException("The parameter " + keyWithoutTabliAndConnection + " derived from the environment variable (" + key + ", short) is unknown for the connection " + connectionName + ". We were expecting one of: " + tabular.toPublicListOfParameters(attributeEnums));

      }

    }
  }

  /**
   * By default, the user home (trick to not show the user in the path in the doc)
   */
  public static Path determineUserHome(Vault vault, TabularEnvs tabularEnvs, Map<TabularAttributeEnum, com.tabulify.conf.Attribute> attributeMap) {

    TabularAttributeEnum userHome = TabularAttributeEnum.USER_HOME;
    Vault.VariableBuilder confVariable = vault.createVariableBuilderFromAttribute(userHome);

    KeyNormalizer osEnvName = tabularEnvs.getNormalizedKey(userHome);
    String sysConfPathString = tabularEnvs.getJavaSysValue(osEnvName);
    if (sysConfPathString != null) {
      com.tabulify.conf.Attribute attribute = confVariable
        .setOrigin(Origin.SYS)
        .buildSafe(sysConfPathString);
      attributeMap.put(userHome, attribute);
      return Paths.get(attribute.getValueOrDefaultAsStringNotNull());
    }


    String confPathString = tabularEnvs.getOsEnvValue(osEnvName);
    if (confPathString != null) {
      com.tabulify.conf.Attribute attribute = confVariable
        .setOrigin(com.tabulify.conf.Origin.OS)
        .buildSafe(confPathString);
      attributeMap.put(userHome, attribute);
      return Paths.get(attribute.getValueOrDefaultAsStringNotNull());
    }

    Path defaultValue = Fs.getUserHome().resolve("." + TABLI_NAME);
    com.tabulify.conf.Attribute attribute = confVariable
      .setOrigin(Origin.DEFAULT)
      .buildSafe(defaultValue);
    attributeMap.put(userHome, attribute);
    return defaultValue;
  }

  /**
   * @param passphrase - tabular signature passphrase
   */
  public static String determinePassphrase(String passphrase) {

    if (passphrase != null) {
      return passphrase;
    }
    /**
     * We determine without using {@link TabularEnvs}
     * because passphrase is mandatory to initiate a vault
     */
    String normalizedPassphraseName = (TABLI_NAME + "_" + TabularAttributeEnum.PASSPHRASE).toLowerCase();
    for (Map.Entry<String, String> osEnv : System.getenv().entrySet()) {

      String key = osEnv.getKey();
      if (key.toLowerCase().equals(normalizedPassphraseName)) {
        String value = osEnv.getValue();
        if (value.startsWith(Vault.VAULT_PREFIX)) {
          throw new RuntimeException("The passphrase os env (" + key + ") cannot have an encrypted value");
        }
        return value;
      }
    }
    return null;
  }

  public static TabularLogLevel determineLogLevel(TabularLogLevel logLevel, Vault vault, TabularEnvs tabularEnvs, Map<TabularAttributeEnum, Attribute> attributeMap) {

    TabularAttributeEnum logLevelAttribute = TabularAttributeEnum.LOG_LEVEL;
    Vault.VariableBuilder confVariable = vault.createVariableBuilderFromAttribute(logLevelAttribute);

    if (logLevel != null) {
      com.tabulify.conf.Attribute attribute = confVariable
        .setOrigin(Origin.COMMAND_LINE)
        .buildSafe(logLevel);
      attributeMap.put(logLevelAttribute, attribute);
      return logLevel;
    }

    KeyNormalizer logLevelKeyNormalized = tabularEnvs.getNormalizedKey(logLevelAttribute);
    String logLevelJavaSys = tabularEnvs.getJavaSysValue(logLevelKeyNormalized);
    if (logLevelJavaSys != null) {
      try {
        logLevel = Casts.cast(logLevelJavaSys, TabularLogLevel.class);
      } catch (CastException e) {
        throw new RuntimeException("The log level value " + logLevelJavaSys + " of the java sys env " + logLevelKeyNormalized + " is not a valid value. Valid values are: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TabularLogLevel.class), e);
      }
      Attribute attribute = confVariable
        .setOrigin(Origin.SYS)
        .buildSafe(logLevel);
      attributeMap.put(logLevelAttribute, attribute);
      return logLevel;
    }

    String logLevelOs = tabularEnvs.getOsEnvValue(logLevelKeyNormalized);
    if (logLevelOs != null) {
      try {
        logLevel = Casts.cast(logLevelOs, TabularLogLevel.class);
      } catch (CastException e) {
        throw new RuntimeException("The log level value " + logLevelOs + " of the os env " + logLevelKeyNormalized + " is not a valid value. Valid values are: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TabularLogLevel.class), e);
      }
      com.tabulify.conf.Attribute attribute = confVariable
        .setOrigin(com.tabulify.conf.Origin.OS)
        .buildSafe(logLevel);
      attributeMap.put(logLevelAttribute, attribute);
      return logLevel;
    }

    return (TabularLogLevel) TabularAttributeEnum.LOG_LEVEL.getDefaultValue();
  }
}
