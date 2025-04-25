package com.tabulify;

import net.bytle.conf.ConfManager;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NoValueException;
import net.bytle.exception.NoVariableException;
import net.bytle.java.JavaEnvs;
import net.bytle.type.*;
import net.bytle.type.env.DotEnv;
import net.bytle.type.env.OsEnvs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TabularVariables {


  private final Map<String, Variable> variables = new MapKeyIndependent<>();


  public TabularVariables(Tabular tabular, ProjectConfigurationFile projectConfigurationFile) {

    /**
     * Attach value provider to tabular variables
     */
    Set<Variable> tabularVariables = new HashSet<>();
    TabularAttributes[] tabularAttributes = TabularAttributes.values();
    for (TabularAttributes tabularAttribute : tabularAttributes) {
      Variable variable = Variable.create(tabularAttribute, Origin.INTERNAL);
      tabularVariables.add(variable);
      switch (tabularAttribute) {
        case IS_DEV:
          boolean isDev = tabular.getExecutionEnvironment().equals(TabularExecEnv.DEV);
          variable
            .setOriginalValue(isDev)
            .setValueProvider(() -> isDev);
          break;
        case TABULIFY_HOME_PATH:
          variable
            .setOriginalValue(JavaEnvs.HOME_PATH)
            .setValueProvider(tabular::getHomePath);
          break;
        case PROJECT_ENV:
          variable.setValueProvider(tabular::getExecutionEnvironment);
          break;
        case PROJECT_CONNECTION:
          variable.setValueProvider(() -> {
            if (projectConfigurationFile == null) {
              return null;
            }
            return projectConfigurationFile.getConnectionVaultPath().normalize();
          });
          break;
        case PROJECT_VARIABLE:
          variable.setValueProvider(() -> {
            if (projectConfigurationFile == null) {
              return null;
            }
            return projectConfigurationFile.getVariablesPath().normalize();
          });
          break;
      }
    }

    /**
     * Load the public variables first
     * because they can be overwritten
     */
    Set<Variable> publicVariables = tabularVariables
      .stream()
      .filter(a -> ((TabularAttributes) a.getAttribute()).isPublic())
      .collect(Collectors.toSet());
    loadTabularAttributes(publicVariables);
    loadEnvironmentVariable();
    loadSys();
    loadUserConfigurationFile();
    if (projectConfigurationFile != null) {
      loadProjectConfigurationFile(projectConfigurationFile);
      loadDotEnvConfiguration(projectConfigurationFile);
    }
    loadVariablesArgument(tabular.variablePathArgument);

    /**
     * Load the private variable at the end
     * To overwrite any personal configuration
     */
    Set<Variable> privateVariables = tabularVariables
      .stream()
      .filter(a -> !((TabularAttributes) a.getAttribute()).isPublic())
      .collect(Collectors.toSet());
    loadTabularAttributes(privateVariables);

  }

  private void loadSys() {
    Map<String, Object> sysProperties;
    try {
      sysProperties = Casts.castToSameMap(System.getProperties(), String.class, Object.class)
        .entrySet()
        .stream()
        .collect(Collectors.toMap(
          Map.Entry::getKey,
          Map.Entry::getValue
        ));
    } catch (CastException e) {
      throw new InternalException("String, object should not bring a cast exception", e);
    }
    this.addAllConf(sysProperties, Origin.SYS);
  }

  private void loadVariablesArgument(Path variablePathArgument) {

    if (variablePathArgument == null) {
      return;
    }
    Map<String, Object> confMap;
    try (ConfManager confManager = ConfManager.createFromPath(variablePathArgument)) {
      confMap = confManager.getConfMap();
    }
    this.addAllConf(confMap, Origin.COMMAND_LINE);

  }


  private void loadTabularAttributes(Set<Variable> tabularVariables) {


    this.variables.putAll(
      tabularVariables
        .stream()
        .collect(
          Collectors.toMap(
            Variable::getUniqueName,
            e -> e
          ))
    );

  }

  public static TabularVariables create(Tabular tabular, ProjectConfigurationFile projectConfigurationFile) {
    return new TabularVariables(tabular, projectConfigurationFile);
  }

  private void loadEnvironmentVariable() {
    Map<String, Object> actualEnvEntries;
    try {
      actualEnvEntries = Casts.castToSameMap(OsEnvs.getAll(), String.class, Object.class);
    } catch (CastException e) {
      throw new InternalException("String, object should not bring a cast exception", e);
    }
    Map<String, Object> newConfMap = new HashMap<>();
    Pattern pattern = Pattern.compile("(?i:tabli_)");
    for (Map.Entry<String, Object> actual : actualEnvEntries.entrySet()) {
      String key = pattern.matcher(actual.getKey()).replaceFirst("").toLowerCase(Locale.ROOT).trim();
      newConfMap.put(key, actual.getValue().toString().trim());
    }
    this.addAllConf(newConfMap, Origin.OS);
  }

  private void loadProjectConfigurationFile(ProjectConfigurationFile projectConfigurationFile) {
    if (projectConfigurationFile != null) {
      this.addAllConf(projectConfigurationFile.getVariables(), Origin.PROJECT);
    }
  }

  private void loadDotEnvConfiguration(ProjectConfigurationFile projectConfigurationFile) {

    List<String> envNames = new ArrayList<>();
    String baseEnv = ".env";
    envNames.add(baseEnv);
    for (String envName : envNames) {
      Path dotEnvPath;
      if (projectConfigurationFile != null) {
        dotEnvPath = projectConfigurationFile.getProjectDirectory().resolve(envName);
      } else {
        dotEnvPath = Paths.get(".").resolve(envName);
      }
      if (!Files.exists(dotEnvPath)) {
        DbLoggers.LOGGER_TABULAR_START.info("The dotenv file (" + envName + ") does not exists (" + dotEnvPath + ")");
        continue;
      }
      DotEnv dotenv = DotEnv.createFromPath(dotEnvPath);
      Map<String, Object> confMap;
      try {
        confMap = Casts.castToSameMap(dotenv.getAll(), String.class, Object.class);
      } catch (CastException e) {
        throw new InternalException("String, object should not bring a cast exception", e);
      }
      this.addAllConf(confMap, Origin.DOTENV);
      DbLoggers.LOGGER_TABULAR_START.info("The dotenv file (" + envName + ") was loaded (" + dotEnvPath + ")");
    }

  }

  private TabularVariables addAllConf(Map<String, Object> confMap, Origin tabularVariableOrigin) {

    Map<String, Variable> variables = confMap.entrySet()
      .stream()
      .map(e -> Variable.create(e.getKey(), tabularVariableOrigin)
        .setOriginalValue(e.getValue())
      )
      .collect(Collectors.toMap(
        e -> e.getAttribute().toString(),
        e -> e
      ));
    this.variables.putAll(variables);
    return this;
  }


  public Variable getVariable(String name) {
    return variables.get(name);
  }

  private void loadUserConfigurationFile() {
    /**
     * Load default configuration file
     */
    Map<String, Object> confMap;
    try (ConfManager confManager = ConfManager
      .createFromPath(getUserConfigurationFile())) {
      confMap = confManager.getConfMap();
    }
    this.addAllConf(
      confMap,
      Origin.USER
    );
  }

  public Path getUserConfigurationFile() {
    try {
      return (Path) this.getVariable(TabularAttributes.USER_VARIABLES_FILE).getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      throw new InternalException("USER_VARIABLES_FILE has already a default, it should not happen");
    }
  }

  public Variable getVariable(Attribute attribute) throws NoVariableException {
    Variable variable = this.variables.get(attribute.toString());
    if (variable == null) {
      throw new NoVariableException("The variable (" + attribute + ") was not found");
    }
    return variable;
  }

  public Collection<Variable> getVariables() {
    return this.variables.values();
  }

  public Map<String, Object> getVariablesAsKeyIndependentMap() {
    MapKeyIndependent<Object> returnedMap = new MapKeyIndependent<>();
    for (Variable variable : this.variables.values()) {
      returnedMap.put(variable.getUniqueName(), variable.getValueOrDefaultOrNull());
    }
    return returnedMap;
//    return this.variables.values()
//      .stream()
//      .collect(Collectors.toMap(
//        Variable::getUniqueName,
//        Variable::getOriginalValue,
//        (e1, e2) -> e1,
//        MapKeyIndependent::new
//      ));
  }


}
