package com.tabulify;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.ConfVault;
import com.tabulify.conf.Origin;
import com.tabulify.conf.TabularEnvs;
import com.tabulify.connection.ConnectionHowTos;
import net.bytle.exception.CastException;
import net.bytle.fs.Fs;
import net.bytle.java.JavaEnvs;
import net.bytle.java.Javas;
import net.bytle.type.Casts;
import net.bytle.type.Enums;
import net.bytle.type.KeyNormalizer;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static com.tabulify.Tabular.TABLI_USER_HOME_PATH;

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
      DbLoggers.LOGGER_TABULAR_START.info("Tabli env: Passed as argument " + env);
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
     * Os
     */
    KeyNormalizer osEnvName = tabularEnvs.getOsTabliEnvName(attribute);
    String envOsValue = tabularEnvs.getOsEnvValue(osEnvName);
    if (envOsValue != null) {
      try {
        DbLoggers.LOGGER_TABULAR_START.info("Tabli env: Found in OS env " + osEnvName.toEnvName() + " with the value " + envOsValue);
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
      DbLoggers.LOGGER_TABULAR_START.info("Tabli env: IDE as it's a junit run");
      value = TabularExecEnv.IDE;
      com.tabulify.conf.Attribute variable = configVariable
        .setOrigin(com.tabulify.conf.Origin.RUNTIME)
        .buildSafe(value);
      attributeMap.put((TabularAttributeEnum) variable.getAttributeMetadata(), variable);
      return value;
    }

    DbLoggers.LOGGER_TABULAR_START.info("Tabli env: Default to dev");
    value = TabularExecEnv.DEV;
    com.tabulify.conf.Attribute variable = configVariable
      .setOrigin(com.tabulify.conf.Origin.RUNTIME)
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

    // Env
    KeyNormalizer envName = tabularEnvs.getOsTabliEnvName(TabularAttributeEnum.HOME);
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
          .setOrigin(com.tabulify.conf.Origin.RUNTIME)
          .buildSafe(closestHomePath);
        attributeMap.put((TabularAttributeEnum) variable.getAttributeMetadata(), variable);
        return closestHomePath;
      } catch (FileNotFoundException e) {
        // Not found
      }

    }

    // in prod, the class are in the jars directory
    Path prodHomePath = Javas.getSourceCodePath(ConnectionHowTos.class).getParent();
    com.tabulify.conf.Attribute variable = variableBuilder
      .setOrigin(com.tabulify.conf.Origin.RUNTIME)
      .buildSafe(prodHomePath);
    attributeMap.put((TabularAttributeEnum) variable.getAttributeMetadata(), variable);
    return prodHomePath;

  }


  static public Path determineProjectHome(Path projectHomePath, Vault vault, Map<TabularAttributeEnum, com.tabulify.conf.Attribute> attributeMap, TabularEnvs tabularEnvs) {

    TabularAttributeEnum attribute = TabularAttributeEnum.PROJECT_HOME;
    Vault.VariableBuilder confVariable = vault.createVariableBuilderFromAttribute(attribute);

    if (projectHomePath != null) {
      com.tabulify.conf.Attribute variable = confVariable
        .setOrigin(com.tabulify.conf.Origin.COMMAND_LINE)
        .buildSafe(projectHomePath);
      attributeMap.put((TabularAttributeEnum) variable.getAttributeMetadata(), variable);
      return projectHomePath;
    }

    // Env
    KeyNormalizer envName = tabularEnvs.getOsTabliEnvName(attribute);
    String projectHomeFromEnv = tabularEnvs.getOsEnvValue(envName);
    if (projectHomeFromEnv != null) {
      com.tabulify.conf.Attribute variable = confVariable
        .setOrigin(com.tabulify.conf.Origin.OS)
        .buildSafe(projectHomeFromEnv);
      attributeMap.put((TabularAttributeEnum) variable.getAttributeMetadata(), variable);
      return Paths.get(projectHomeFromEnv);
    }

    // Derived
    confVariable.setOrigin(com.tabulify.conf.Origin.RUNTIME);
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

  static public Path determineConfPath(Path confPath, Vault vault, TabularEnvs tabularEnvs, Path projectHome) {

    Vault.VariableBuilder confVariable = vault.createVariableBuilderFromAttribute(TabularAttributeEnum.CONF);
    if (confPath != null) {
      confVariable
        .setOrigin(com.tabulify.conf.Origin.COMMAND_LINE)
        .buildSafe(confPath.toString());
      return confPath;
    }

    KeyNormalizer osEnvName = tabularEnvs.getOsTabliEnvName(TabularAttributeEnum.CONF);
    String confPathString = tabularEnvs.getOsEnvValue(osEnvName);
    if (confPathString != null) {
      com.tabulify.conf.Attribute attribute = confVariable
        .setOrigin(com.tabulify.conf.Origin.OS)
        .buildSafe(confPathString);
      return Paths.get(attribute.getValueOrDefaultAsStringNotNull());
    }

    if (projectHome != null) {
      Path resolve = projectHome.resolve(Tabular.TABLI_CONF_FILE_NAME);
      confVariable
        .setOrigin(com.tabulify.conf.Origin.RUNTIME)
        .buildSafe(resolve);
      return resolve;
    }

    Path resolve = TABLI_USER_HOME_PATH.resolve(Tabular.TABLI_CONF_FILE_NAME);
    confVariable
      .setOrigin(Origin.RUNTIME)
      .buildSafe(resolve);
    return resolve;

  }


  /**
   * Loop over all env and check that attributes were created
   * Not really needed, but it helps with a bad typo
   */
  public static void checkForEnvNotProcessed(TabularEnvs tabularEnvs, Map<TabularAttributeEnum, com.tabulify.conf.Attribute> attributeMap) {
    for (Map.Entry<String, String> tabularEnv : tabularEnvs.getEnvs().entrySet()) {
      String key = tabularEnv.getKey();
      String lowerCaseKey = key.toLowerCase();
      if (!lowerCaseKey.startsWith(Tabular.TABLI_NAME)) {
        continue;
      }
      String tabularEnvAsString = lowerCaseKey.substring(Tabular.TABLI_NAME.length());
      TabularAttributeEnum tabularAttributes;
      try {
        tabularAttributes = Casts.cast(tabularEnvAsString, TabularAttributeEnum.class);
      } catch (CastException e) {
        throw new RuntimeException("The system env variable (" + key + ") is not a valid tabulify env. Only the following values are excepted: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TabularAttributeEnum.class), e);
      }
      com.tabulify.conf.Attribute attribute = attributeMap.get(tabularAttributes);
      if (attribute == null) {
        throw new RuntimeException("Internal error: The tabulify attribute (" + tabularAttributes + ") was not initialized");
      }
    }
  }

  /**
   * By default, the user home (trick to not show the user in the path in the doc)
   */
  public static Path determineSqliteHome(Vault vault, TabularEnvs tabularEnvs, Map<TabularAttributeEnum, com.tabulify.conf.Attribute> attributeMap) {

    TabularAttributeEnum sqliteHome = TabularAttributeEnum.SQLITE_HOME;
    Vault.VariableBuilder confVariable = vault.createVariableBuilderFromAttribute(sqliteHome);

    KeyNormalizer osEnvName = tabularEnvs.getOsTabliEnvName(sqliteHome);
    String confPathString = tabularEnvs.getOsEnvValue(osEnvName);
    if (confPathString != null) {
      com.tabulify.conf.Attribute attribute = confVariable
        .setOrigin(com.tabulify.conf.Origin.OS)
        .buildSafe(confPathString);
      attributeMap.put(sqliteHome, attribute);
      return Paths.get(attribute.getValueOrDefaultAsStringNotNull());
    }

    Path defaultValue = TABLI_USER_HOME_PATH;
    com.tabulify.conf.Attribute attribute = confVariable
      .setOrigin(Origin.RUNTIME)
      .buildSafe(defaultValue);
    attributeMap.put(sqliteHome, attribute);
    return defaultValue;
  }

  /**
   * @param passphrase - tabular signature passphrase
   */
  public static String determinePassphrase(String passphrase) {

    if (passphrase != null) {
      return passphrase;
    }
    String normalizedPassphraseName = (Tabular.TABLI_NAME + "_" + TabularAttributeEnum.PASSPHRASE).toLowerCase();
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
        .setOrigin(com.tabulify.conf.Origin.OS)
        .buildSafe(logLevel);
      attributeMap.put(logLevelAttribute, attribute);
      return logLevel;
    }


    KeyNormalizer osEnvName = tabularEnvs.getOsTabliEnvName(logLevelAttribute);
    String logLevelOs = tabularEnvs.getOsEnvValue(osEnvName);
    if (logLevelOs != null) {
      try {
        logLevel = Casts.cast(logLevelOs, TabularLogLevel.class);
      } catch (CastException e) {
        throw new RuntimeException("The log level value " + logLevelOs + " of the os env " + osEnvName + " is not a valid value. Valid values are: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TabularLogLevel.class), e);
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
