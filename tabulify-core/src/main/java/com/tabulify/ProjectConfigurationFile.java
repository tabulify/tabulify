package com.tabulify;

import net.bytle.conf.ConfManager;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.fs.Fs;
import net.bytle.type.Casts;
import org.yaml.snakeyaml.Yaml;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ProjectConfigurationFile {


  public static final String PROJECT_CONF_FILE_NAME = ".tabli.yml";
  private final Path path;
  private final String connectionsRelativePath;
  private final String variablesRelativePath;


  public ProjectConfigurationFile(Path path) {
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

    if (input.trim().isEmpty()) {
      connectionsRelativePath = null;
      variablesRelativePath = null;
      return;
    }

    Object yamlObject = yaml.loadAll(input).iterator().next();
    Map<String, Object> yamlRootMap;
    try {
      yamlRootMap = Casts.castToSameMap(yamlObject, String.class, Object.class);
    } catch (CastException e) {
      throw new InternalException("String and Object should not throw a cast exception", e);
    }


    Map<String, Object> environmentMaps = Map.of();
    Object environmentObjects = yamlRootMap.get("envs");
    if (environmentObjects != null) {
      try {
        environmentMaps = Casts.castToSameMap(environmentObjects, String.class, Object.class);
      } catch (CastException e) {
        throw new InternalException("String and Object should not throw a cast exception", e);
      }
    }

    this.connectionsRelativePath = (String) environmentMaps.get("connections");
    this.variablesRelativePath = (String) environmentMaps.get("variables");


  }

  public static ProjectConfigurationFile createFrom(Path path) throws FileNotFoundException {
    return new ProjectConfigurationFile(path);
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
        .resolve(TabularAttributes.CONNECTION_VAULT.getDefaultValue().toString());
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
    if (Files.exists(projectConf)) {
      try(ConfManager fromPath = ConfManager.createFromPath(projectConf)) {
        envs.putAll(fromPath.getConfMap());
      }
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

}
