package net.bytle.db;

import net.bytle.conf.ConfManager;
import net.bytle.fs.Fs;
import net.bytle.os.Oss;
import net.bytle.type.Casts;
import org.yaml.snakeyaml.Yaml;

import java.io.FileNotFoundException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ProjectConfigurationFile {


  public static final String DEVELOPMENT_ENV = "development";
  public static final String PRODUCTION_ENV = "production";
  public static final String NONE_ENV = "None";
  private final Path path;
  private final String connectionsRelativePath;
  private final String variablesRelativePath;
  private final String environment;


  public ProjectConfigurationFile(Path path, String env) {
    this.path = path;

    if (Files.isDirectory(path)) {
      throw new IllegalArgumentException("The project configuration file given (" + path + ") is not a file but a directory");
    }

    Yaml yaml = new Yaml();
    String input;
    try {
      input = Fs.getFileContent(path);
    } catch (NoSuchFileException e) {
      throw new RuntimeException("The project configuration file (" + path + ") does not exists");
    }

    if (input.trim().equals("")) {
      connectionsRelativePath = null;
      variablesRelativePath = null;
      environment = NONE_ENV;
      return;
    }

    Object yamlObject = yaml.loadAll(input).iterator().next();
    Map<String, Object> yamlRootMap = Casts.castToSameMap(yamlObject, String.class, Object.class);
    Object environmentObject = null;

    Object environmentObjects = yamlRootMap.get("envs");
    if (environmentObjects != null) {
      Map<String, Object> environmentMaps = Casts.castToSameMap(environmentObjects, String.class, Object.class);
      String environment = DEVELOPMENT_ENV;
      if (env != null) {
        environmentObject = environmentMaps.get(env);
        environment = env;
      } else {

        String fqdn;
        try {
          fqdn = Oss.getFqdn();
          environment = fqdn;
          environmentObject = environmentMaps.get(fqdn);
        } catch (UnknownHostException e) {
          DbLoggers.LOGGER_DB_ENGINE.warning("Project File: We couldn't get the full qualified name of your machine to determine your environment. Error: " + e.getMessage());
        }
        if (environmentObject == null) {
          environment = DEVELOPMENT_ENV;
          environmentObject = environmentMaps.get(DEVELOPMENT_ENV);
        }
      }
      this.environment = environment;
    } else {
      this.environment = NONE_ENV;
    }


    if (environmentObject == null) {
      connectionsRelativePath = null;
      variablesRelativePath = null;
      return;
    }

    Map<String, String> environmentMap = Casts.castToSameMap(environmentObject, String.class, String.class);
    this.connectionsRelativePath = environmentMap.get("connections");
    this.variablesRelativePath = environmentMap.get("variables");


  }

  public static ProjectConfigurationFile createFrom(Path path, String env) throws FileNotFoundException {
    return new ProjectConfigurationFile(path, env);
  }

  public Path getProjectDirectory() {
    return this.path.getParent();
  }

  public Path getConnectionVaultPath() {
    Path projectConnectionVault;
    if (this.connectionsRelativePath == null) {
      projectConnectionVault = this
        .getProjectDirectory()
        .resolve(TabularAttributes.PROJECT_CONF_DIR_NAME.getDefaultValue().toString())
        .resolve(TabularAttributes.CONNECTION_VAULT_NAME.getDefaultValue().toString());
    } else {
      projectConnectionVault = this
        .getProjectDirectory()
        .resolve(this.connectionsRelativePath);
    }
    return projectConnectionVault;
  }

  @Override
  public String toString() {
    return this.path.getParent().getFileName().toString();
  }

  public Map<String, Object> getVariables() {
    Path projectConf = this.getVariablesPath();
    Map<String, Object> envs = new HashMap<>();
    envs.put(TabularAttributes.PROJECT_ENV.toString(), this.environment);
    if (Files.exists(projectConf)) {
      envs.putAll(ConfManager.createFromPath(projectConf).getConfMap());
    }
    return envs;
  }

  public Path getVariablesPath() {
    if (variablesRelativePath == null) {
      return this.getProjectDirectory()
        .resolve(TabularAttributes.PROJECT_CONF_DIR_NAME.getDefaultValue().toString())
        .resolve(TabularAttributes.VARS_FILE_NAME.getDefaultValue().toString());
    } else {
      return this.getProjectDirectory()
        .resolve(variablesRelativePath);
    }
  }


  public String getEnvironment() {
    return environment;
  }
}
