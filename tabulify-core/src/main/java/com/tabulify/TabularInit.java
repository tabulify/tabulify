package com.tabulify;

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


  static TabularExecEnv determineEnv(TabularExecEnv env, Vault vault, TabularEnvs tabularEnvs, Map<TabularAttribute, com.tabulify.conf.Attribute> variables, ConfVault confVault) {

    TabularAttribute attribute = TabularAttribute.ENV;
    Vault.VariableBuilder configVariable = vault.createVariableBuilderFromAttribute(attribute);
    TabularExecEnv value;

    // Env
    if (env != null) {
      DbLoggers.LOGGER_TABULAR_START.info("Tabli env: Passed as argument " + env);
      com.tabulify.conf.Attribute variable = configVariable
        .setOrigin(com.tabulify.conf.Origin.COMMAND_LINE)
        .buildSafe(env.toString());
      variables.put((TabularAttribute) variable.getAttributeMetadata(), variable);
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
      variables.put((TabularAttribute) confAttribute.getAttributeMetadata(), confAttribute);
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
        variables.put((TabularAttribute) variable.getAttributeMetadata(), variable);
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
      variables.put((TabularAttribute) variable.getAttributeMetadata(), variable);
      return value;
    }

    DbLoggers.LOGGER_TABULAR_START.info("Tabli env: Default to dev");
    value = TabularExecEnv.DEV;
    com.tabulify.conf.Attribute variable = configVariable
      .setOrigin(com.tabulify.conf.Origin.RUNTIME)
      .buildSafe(value);
    variables.put((TabularAttribute) variable.getAttributeMetadata(), variable);
    return value;

  }


  /**
   * @param homePath the home path from the constructor
   */
  static Path determineHomePath(Path homePath, TabularExecEnv execEnv, TabularEnvs tabularEnvs, Map<TabularAttribute, com.tabulify.conf.Attribute> variables, Vault vault, ConfVault confVault) {

    TabularAttribute attribute = TabularAttribute.HOME;
    Vault.VariableBuilder variableBuilder = vault.createVariableBuilderFromAttribute(attribute);

    if (homePath != null) {
      com.tabulify.conf.Attribute variable = variableBuilder
        .setOrigin(com.tabulify.conf.Origin.COMMAND_LINE)
        .buildSafe(homePath);
      variables.put((TabularAttribute) variable.getAttributeMetadata(), variable);
      return homePath;
    }

    /**
     * Conf Manager
     */
    com.tabulify.conf.Attribute confHomeAttribute = confVault.getAttribute(attribute);
    if (confHomeAttribute != null) {
      String confEnvValue = confHomeAttribute.getValueOrDefaultAsStringNotNull();
      variables.put((TabularAttribute) confHomeAttribute.getAttributeMetadata(), confHomeAttribute);
      return Paths.get(confEnvValue);
    }

    // Env
    KeyNormalizer envName = tabularEnvs.getOsTabliEnvName(TabularAttribute.HOME);
    String tabliHome = tabularEnvs.getOsEnvValue(envName);
    if (tabliHome != null) {
      com.tabulify.conf.Attribute variable = variableBuilder
        .setOrigin(com.tabulify.conf.Origin.OS)
        .buildSafe(tabliHome);
      variables.put((TabularAttribute) variable.getAttributeMetadata(), variable);
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
        variables.put((TabularAttribute) variable.getAttributeMetadata(), variable);
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
    variables.put((TabularAttribute) variable.getAttributeMetadata(), variable);
    return prodHomePath;

  }


  static public Path determineProjectHome(Path projectHomePath, Vault vault, Map<TabularAttribute, com.tabulify.conf.Attribute> variables, TabularEnvs tabularEnvs) {

    TabularAttribute attribute = TabularAttribute.PROJECT_HOME;
    Vault.VariableBuilder confVariable = vault.createVariableBuilderFromAttribute(attribute);

    if (projectHomePath != null) {
      com.tabulify.conf.Attribute variable = confVariable
        .setOrigin(com.tabulify.conf.Origin.COMMAND_LINE)
        .buildSafe(projectHomePath);
      variables.put((TabularAttribute) variable.getAttributeMetadata(), variable);
      return projectHomePath;
    }

    // Env
    KeyNormalizer envName = tabularEnvs.getOsTabliEnvName(attribute);
    String projectHomeFromEnv = tabularEnvs.getOsEnvValue(envName);
    if (projectHomeFromEnv != null) {
      com.tabulify.conf.Attribute variable = confVariable
        .setOrigin(com.tabulify.conf.Origin.OS)
        .buildSafe(projectHomeFromEnv);
      variables.put((TabularAttribute) variable.getAttributeMetadata(), variable);
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
      variables.put((TabularAttribute) variable.getAttributeMetadata(), variable);
      return closestProjectHomePath;
    } catch (FileNotFoundException e) {
      // not a project
      com.tabulify.conf.Attribute variable = confVariable.buildSafe(null);
      variables.put((TabularAttribute) variable.getAttributeMetadata(), variable);
      return null;
    }
  }

  static public Path determineConfPath(Path confPath, Vault vault, TabularEnvs tabularEnvs, Path projectHome) {

    Vault.VariableBuilder confVariable = vault.createVariableBuilderFromAttribute(TabularAttribute.CONF);
    if (confPath != null) {
      confVariable
        .setOrigin(com.tabulify.conf.Origin.COMMAND_LINE)
        .buildSafe(confPath.toString());
      return confPath;
    }

    KeyNormalizer osEnvName = tabularEnvs.getOsTabliEnvName(TabularAttribute.CONF);
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
  public static void checkForEnvNotProcessed(TabularEnvs tabularEnvs, Map<TabularAttribute, com.tabulify.conf.Attribute> variables) {
    for (Map.Entry<String, String> tabularEnv : tabularEnvs.getEnvs().entrySet()) {
      String key = tabularEnv.getKey();
      String lowerCaseKey = key.toLowerCase();
      if (!lowerCaseKey.startsWith(Tabular.TABLI_NAME)) {
        continue;
      }
      String tabularEnvAsString = lowerCaseKey.substring(Tabular.TABLI_NAME.length());
      TabularAttribute tabularAttributes;
      try {
        tabularAttributes = Casts.cast(tabularEnvAsString, TabularAttribute.class);
      } catch (CastException e) {
        throw new RuntimeException("The system env variable (" + key + ") is not a valid tabulify env. Only the following values are excepted: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TabularAttribute.class), e);
      }
      com.tabulify.conf.Attribute attribute = variables.get(tabularAttributes);
      if (attribute == null) {
        throw new RuntimeException("Internal error: The tabulify attribute (" + tabularAttributes + ") was not initialized");
      }
    }
  }

}
