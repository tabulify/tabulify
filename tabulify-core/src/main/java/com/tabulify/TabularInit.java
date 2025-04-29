package com.tabulify;

import com.tabulify.connection.ConnectionHowTos;
import net.bytle.exception.CastException;
import net.bytle.fs.Fs;
import net.bytle.java.JavaEnvs;
import net.bytle.java.Javas;
import net.bytle.type.*;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.tabulify.Tabular.USER_HOME_PATH;

/**
 * Just a class to store all init procedures
 * so that the tabular object is uncluttered
 */
public class TabularInit {


  private final MapKeyIndependent<String> env;
  private final MapKeyIndependent<String> sysProperties;

  public TabularInit() {
    this.env = MapKeyIndependent.createFrom(System.getenv(), String.class);
    this.sysProperties = MapKeyIndependent.createFrom(System.getProperties(), String.class);
  }

  TabularExecEnv determineEnv(TabularExecEnv env, Vault vault) {

    TabularAttributes attribute = TabularAttributes.ENV;
    Vault.ConfVariable confVariable = vault.confVariable(attribute);

    // Env
    if (env != null) {
      DbLoggers.LOGGER_TABULAR_START.info("Tabli env: Passed as argument " + env);
      confVariable
        .setOrigin(Origin.COMMAND_LINE)
        .build(env.toString());
      return env;
    }

    KeyNormalizer osEnvName = this.getOsEnvName(attribute);
    String envOsValue = this.getOsEnvValue(osEnvName);
    TabularExecEnv value;
    if (envOsValue != null) {
      try {
        DbLoggers.LOGGER_TABULAR_START.info("Tabli env: Found in OS env " + osEnvName.toEnvName() + " with the value " + envOsValue);
        value = Casts.cast(envOsValue, TabularExecEnv.class);
        confVariable
          .setOrigin(Origin.OS)
          .build(value.toString());
        return value;
      } catch (CastException e) {
        throw new IllegalArgumentException("The os env (" + osEnvName.toEnvName() + ") has a env value (" + envOsValue + ") that is unknown. Possible values: " + Enums.toConstantAsStringCommaSeparated(TabularExecEnv.class), e);
      }
    }

    if (JavaEnvs.isJUnitTest()) {
      DbLoggers.LOGGER_TABULAR_START.info("Tabli env: IDE as it's a junit run");
      value = TabularExecEnv.IDE;
      confVariable
        .setOrigin(Origin.INTERNAL)
        .build(value.toString());
      return value;
    }

    DbLoggers.LOGGER_TABULAR_START.info("Tabli env: Default to dev");
    value = TabularExecEnv.DEV;
    confVariable
      .setOrigin(Origin.INTERNAL)
      .build(value.toString());
    return value;

  }

  private String getOsEnvValue(KeyNormalizer keyNormalize) {
    return this.env.get(keyNormalize);
  }

  private KeyNormalizer getOsEnvName(TabularAttributes tabularAttributes) {
    return KeyNormalizer.create(Tabular.TABLI_PREFIX + "_" + tabularAttributes);
  }


  /**
   * @param homePath the home path from the init
   */
  Path determineHomePath(Path homePath, TabularExecEnv execEnv) {

    if (homePath != null) {
      return homePath;
    }

    // Env
    KeyNormalizer envName = this.getOsEnvName(TabularAttributes.HOME);
    String tabliHome = this.getOsEnvValue(envName);
    if (tabliHome != null) {
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
        return Fs.closest(Paths.get("."), ".git").getParent();
      } catch (FileNotFoundException e) {
        // Not found
      }

    }

    // in prod, the class are in the jars directory
    return Javas.getSourceCodePath(ConnectionHowTos.class).getParent();

  }


  public Path determineProjectHome(Path projectHomePath) {
    if (projectHomePath != null) {
      return projectHomePath;
    }
    try {
      return Fs.closest(Paths.get("."), Tabular.TABLI_CONF_FILE_NAME).getParent();
    } catch (FileNotFoundException e) {
      // not a project
      return null;
    }
  }

  public Path determineConfPath(Path confPath, Vault vault, Path projectHome) {

    Vault.ConfVariable confVariable = vault.confVariable(TabularAttributes.CONF);
    if (confPath != null) {
      confVariable
        .setOrigin(Origin.COMMAND_LINE)
        .build(confPath.toString());
      return confPath;
    }

    KeyNormalizer osEnvName = this.getOsEnvName(TabularAttributes.CONF);
    String confPathString = this.getOsEnvValue(osEnvName);
    if (confPathString != null) {
      Variable variable = confVariable
        .setOrigin(Origin.OS)
        .build(confPathString);
      return Paths.get(variable.getValueOrDefaultAsStringNotNull());
    }

    if (projectHome != null) {
      Path resolve = projectHome.resolve(Tabular.TABLI_CONF_FILE_NAME);
      confVariable
        .setOrigin(Origin.PROJECT)
        .build(resolve.toString());
      return resolve;
    }

    Path resolve = USER_HOME_PATH.resolve(Tabular.TABLI_CONF_FILE_NAME);
    confVariable
      .setOrigin(Origin.INTERNAL)
      .build(resolve.toString());
    return resolve;

  }

}
