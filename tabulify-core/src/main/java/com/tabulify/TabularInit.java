package com.tabulify;

import com.tabulify.conf.ConfVault;
import com.tabulify.conf.TabularEnvs;
import com.tabulify.connection.ConnectionHowTos;
import net.bytle.exception.CastException;
import net.bytle.fs.Fs;
import net.bytle.java.JavaEnvs;
import net.bytle.java.Javas;
import net.bytle.type.*;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tabulify.Tabular.TABLI_USER_HOME_PATH;

/**
 * Just a class to store all init procedures
 * so that the tabular object is uncluttered
 */
public class TabularInit {


  static TabularExecEnv determineEnv(TabularExecEnv env, Vault vault, TabularEnvs tabularEnvs, Map<TabularAttribute, Variable> variables, ConfVault confVault) {

    TabularAttribute attribute = TabularAttribute.ENV;
    Vault.VariableBuilder configVariable = vault.createVariableBuilderFromAttribute(attribute);
    TabularExecEnv value;

    // Env
    if (env != null) {
      DbLoggers.LOGGER_TABULAR_START.info("Tabli env: Passed as argument " + env);
      Variable variable = configVariable
        .setOrigin(Origin.COMMAND_LINE)
        .buildSafe(env.toString());
      variables.put((TabularAttribute) variable.getAttribute(), variable);
      return env;
    }

    /**
     * Conf Manager
     */
    Variable confVariable = confVault.getVariable(attribute);
    if (confVariable != null) {
      String confEnvValue = confVariable.getValueOrDefaultAsStringNotNull();
      try {
        value = Casts.cast(confEnvValue, TabularExecEnv.class);
      } catch (CastException e) {
        throw new RuntimeException("The env value (" + confEnvValue + ") in the conf file is not correct. We were expecting one of: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TabularExecEnv.class), e);
      }
      variables.put((TabularAttribute) confVariable.getAttribute(), confVariable);
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
        Variable variable = configVariable
          .setOrigin(Origin.OS)
          .build(value.toString());
        variables.put((TabularAttribute) variable.getAttribute(), variable);
        return value;
      } catch (CastException e) {
        throw new IllegalArgumentException("The os env (" + osEnvName.toEnvName() + ") has a env value (" + envOsValue + ") that is unknown. Possible values: " + Enums.toConstantAsStringCommaSeparated(TabularExecEnv.class), e);
      }
    }

    if (JavaEnvs.isJUnitTest()) {
      DbLoggers.LOGGER_TABULAR_START.info("Tabli env: IDE as it's a junit run");
      value = TabularExecEnv.IDE;
      Variable variable = configVariable
        .setOrigin(Origin.RUNTIME)
        .buildSafe(value);
      variables.put((TabularAttribute) variable.getAttribute(), variable);
      return value;
    }

    DbLoggers.LOGGER_TABULAR_START.info("Tabli env: Default to dev");
    value = TabularExecEnv.DEV;
    Variable variable = configVariable
      .setOrigin(Origin.RUNTIME)
      .buildSafe(value);
    variables.put((TabularAttribute) variable.getAttribute(), variable);
    return value;

  }


  /**
   * @param homePath the home path from the constructor
   */
  static Path determineHomePath(Path homePath, TabularExecEnv execEnv, TabularEnvs tabularEnvs, Map<TabularAttribute, Variable> variables, Vault vault, ConfVault confVault) {

    TabularAttribute attribute = TabularAttribute.HOME;
    Vault.VariableBuilder variableBuilder = vault.createVariableBuilderFromAttribute(attribute);

    if (homePath != null) {
      Variable variable = variableBuilder
        .setOrigin(Origin.COMMAND_LINE)
        .buildSafe(homePath);
      variables.put((TabularAttribute) variable.getAttribute(), variable);
      return homePath;
    }

    /**
     * Conf Manager
     */
    Variable confHomeVariable = confVault.getVariable(attribute);
    if (confHomeVariable != null) {
      String confEnvValue = confHomeVariable.getValueOrDefaultAsStringNotNull();
      variables.put((TabularAttribute) confHomeVariable.getAttribute(), confHomeVariable);
      return Paths.get(confEnvValue);
    }

    // Env
    KeyNormalizer envName = tabularEnvs.getOsTabliEnvName(TabularAttribute.HOME);
    String tabliHome = tabularEnvs.getOsEnvValue(envName);
    if (tabliHome != null) {
      Variable variable = variableBuilder
        .setOrigin(Origin.OS)
        .buildSafe(tabliHome);
      variables.put((TabularAttribute) variable.getAttribute(), variable);
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
        Variable variable = variableBuilder
          .setOrigin(Origin.RUNTIME)
          .buildSafe(closestHomePath);
        variables.put((TabularAttribute) variable.getAttribute(), variable);
        return closestHomePath;
      } catch (FileNotFoundException e) {
        // Not found
      }

    }

    // in prod, the class are in the jars directory
    Path prodHomePath = Javas.getSourceCodePath(ConnectionHowTos.class).getParent();
    Variable variable = variableBuilder
      .setOrigin(Origin.RUNTIME)
      .buildSafe(prodHomePath);
    variables.put((TabularAttribute) variable.getAttribute(), variable);
    return prodHomePath;

  }


  static public Path determineProjectHome(Path projectHomePath, Vault vault, Map<TabularAttribute, Variable> variables, TabularEnvs tabularEnvs) {

    TabularAttribute attribute = TabularAttribute.PROJECT_HOME;
    Vault.VariableBuilder confVariable = vault.createVariableBuilderFromAttribute(attribute);

    if (projectHomePath != null) {
      Variable variable = confVariable
        .setOrigin(Origin.COMMAND_LINE)
        .buildSafe(projectHomePath);
      variables.put((TabularAttribute) variable.getAttribute(), variable);
      return projectHomePath;
    }

    // Env
    KeyNormalizer envName = tabularEnvs.getOsTabliEnvName(attribute);
    String projectHomeFromEnv = tabularEnvs.getOsEnvValue(envName);
    if (projectHomeFromEnv != null) {
      Variable variable = confVariable
        .setOrigin(Origin.OS)
        .buildSafe(projectHomeFromEnv);
      variables.put((TabularAttribute) variable.getAttribute(), variable);
      return Paths.get(projectHomeFromEnv);
    }

    // Derived
    confVariable.setOrigin(Origin.RUNTIME);
    try {
      Path closestProjectHomePath = Fs.closest(Paths.get("."), Tabular.TABLI_CONF_FILE_NAME).getParent();
      if (closestProjectHomePath == null) {
        // to please the linter as getParent may return null ...
        throw new FileNotFoundException();
      }
      Variable variable = confVariable.buildSafe(closestProjectHomePath.toString());
      variables.put((TabularAttribute) variable.getAttribute(), variable);
      return closestProjectHomePath;
    } catch (FileNotFoundException e) {
      // not a project
      Variable variable = confVariable.buildSafe(null);
      variables.put((TabularAttribute) variable.getAttribute(), variable);
      return null;
    }
  }

  static public Path determineConfPath(Path confPath, Vault vault, TabularEnvs tabularEnvs, Path projectHome) {

    Vault.VariableBuilder confVariable = vault.createVariableBuilderFromAttribute(TabularAttribute.CONF);
    if (confPath != null) {
      confVariable
        .setOrigin(Origin.COMMAND_LINE)
        .buildSafe(confPath.toString());
      return confPath;
    }

    KeyNormalizer osEnvName = tabularEnvs.getOsTabliEnvName(TabularAttribute.CONF);
    String confPathString = tabularEnvs.getOsEnvValue(osEnvName);
    if (confPathString != null) {
      Variable variable = confVariable
        .setOrigin(Origin.OS)
        .buildSafe(confPathString);
      return Paths.get(variable.getValueOrDefaultAsStringNotNull());
    }

    if (projectHome != null) {
      Path resolve = projectHome.resolve(Tabular.TABLI_CONF_FILE_NAME);
      confVariable
        .setOrigin(Origin.RUNTIME)
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
  public static void checkForEnvNotProcessed(TabularEnvs tabularEnvs, Map<TabularAttribute, Variable> variables) {
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
      Variable variable = variables.get(tabularAttributes);
      if (variable == null) {
        throw new RuntimeException("Internal error: The tabulify attribute (" + tabularAttributes + ") was not initialized");
      }
    }
  }


  public static void buildSmtpVariables(TabularEnvs tabularEnvs, Map<TabularAttribute, Variable> variables, Vault vault, ConfVault confVault) {

    List<TabularAttribute> smtpAttributes = Arrays.stream(TabularAttribute.values())
      .filter(a -> a.toString().toLowerCase().startsWith("smtp"))
      .collect(Collectors.toList());
    for (TabularAttribute smtpAttribute : smtpAttributes) {

      Vault.VariableBuilder variableBuilder = vault.createVariableBuilderFromAttribute(smtpAttribute);

      /**
       * Conf Manager
       */
      Variable confHomeVariable = confVault.getVariable(smtpAttribute);
      if (confHomeVariable != null) {
        variables.put((TabularAttribute) confHomeVariable.getAttribute(), confHomeVariable);
        continue;
      }

      // Env
      KeyNormalizer envName = tabularEnvs.getOsTabliEnvName(smtpAttribute);
      String envValue = tabularEnvs.getOsEnvValue(envName);
      if (envValue != null) {
        Variable variable = variableBuilder
          .setOrigin(Origin.OS)
          .buildSafe(envValue);
        variables.put((TabularAttribute) variable.getAttribute(), variable);
        continue;
      }

      // None
      Variable variable = variableBuilder
        .setOrigin(Origin.RUNTIME)
        .buildSafe(null);
      variables.put((TabularAttribute) variable.getAttribute(), variable);

    }


  }
}
