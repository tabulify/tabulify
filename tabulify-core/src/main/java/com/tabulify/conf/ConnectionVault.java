package com.tabulify.conf;


import com.tabulify.DbLoggers;
import com.tabulify.Tabular;
import com.tabulify.Vault;
import com.tabulify.connection.Connection;
import com.tabulify.connection.ConnectionAttribute;
import com.tabulify.connection.ConnectionHowTos;
import com.tabulify.connection.ConnectionOrigin;
import net.bytle.exception.InternalException;
import net.bytle.exception.NoValueException;
import net.bytle.fs.Fs;
import net.bytle.log.Log;
import net.bytle.regexp.Glob;
import net.bytle.type.Casts;
import net.bytle.type.Origin;
import net.bytle.type.SetKeyIndependent;
import net.bytle.type.Variable;
import org.ini4j.Config;
import org.ini4j.Ini;
import org.ini4j.Wini;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A database store implementation based on an ini file
 * Deprecated for {@link ConfVault}
 */
@Deprecated
public class ConnectionVault implements AutoCloseable {


  protected static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;


  private final Tabular tabular;

  private final Path path;

  private final Vault vault;

  /**
   * The in-memory connectionVaultStorage
   * When the init has been done, the value is not null anymore
   */
  private Map<String, Connection> connections = new HashMap<>();


  private ConnectionVault(Tabular tabular, Path path, Vault vault) {

    this.tabular = tabular;
    Objects.requireNonNull(path);
    this.path = path;
    Objects.requireNonNull(vault);
    this.vault = vault;

    // Then load
    if (Files.exists(path)) {
      this.load(path);
    }


  }

  /**
   * @param tabular             - the tabular
   * @param connectionVaultPath - the path
   * @return a connection vault with the tabular vault
   */
  public static ConnectionVault create(Tabular tabular, Path connectionVaultPath) {
    return new ConnectionVault(tabular, connectionVaultPath, tabular.getVault());
  }

  public static ConnectionVault create(Tabular tabular, Path connectionVaultPath, String passphrase) {
    return new ConnectionVault(tabular, connectionVaultPath, Vault.create(passphrase, null));
  }

  public static ConnectionVault create(Tabular tabular, Path connectionVaultPath, Vault vault) {
    return new ConnectionVault(tabular, connectionVaultPath, vault);
  }


  /**
   * Write the changes to the disk
   */
  public void flush() {
    if (this.connections.isEmpty()) {
      // Nothing to flush
      return;
    }
    try {
      /**
       * We are creating a temporary file
       * because if we read the actual connectionVault
       * the ini library will read it
       * and add potentially deleted one in memory.
       */
      Path tempFile = Fs.getTempFilePath("connectionVaultTemp", ".ini");
      Fs.createEmptyFile(tempFile);
      Config config = Config.getGlobal();
      config.setLineSeparator("\n");
      Ini ini = new Ini();
      ini.setConfig(config); // config first otherwise it's not taken into account
      ini.setFile(tempFile.toFile());

      List<Connection> connections = new ArrayList<>(this.connections.values());
      Collections.sort(connections);
      for (Connection connection : connections) {
        String connectionNameSection = connection.toString();
        /**
         * When having a / in the name, it will create intermediate sections
         * because it supports a tree model
         * http://ini4j.sourceforge.net/tutorial/IniTutorial.java.html
         */
        boolean uriFound = false;
        List<Variable> connectionVariables = connection.getVariables().stream().sorted().collect(Collectors.toList());
        for (Variable variable : connectionVariables) {
          if (variable.getAttribute() == ConnectionAttribute.NAME) {
            continue;
          }
          if (variable.getAttribute() == ConnectionAttribute.ORIGIN) {
            // origin is an internal
            continue;
          }
          String valueToStore;
          if (variable.getAttribute() == ConnectionAttribute.URI) {
            uriFound = true;
            valueToStore = (String) variable.getCipherValue();
            if (valueToStore == null) {
              try {
                valueToStore = (String) variable.getValueOrDefault();
              } catch (NoValueException e) {
                throw new InternalException("The URI variable has no value for the connection (" + connection + ")");
              }
            }
          } else {
            Object originalValue = variable.getCipherValue();
            if (originalValue == null) {
              continue;
            }
            valueToStore = originalValue.toString();
          }
          ini.put(connectionNameSection, tabular.toPublicName(variable.getAttribute().toString()), valueToStore);
        }
        if (!uriFound) {
          throw new InternalException("The URI variable was not found for the connection (" + connection + ")");
        }
      }
      ini.store();
      Fs.move(tempFile, this.path, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * Remove all connections metadata information that matches one of the globPatterns
   *
   * @param globPatterns one or more glob pattern
   * @return a list of database name removed
   */
  @SuppressWarnings("UnusedReturnValue")
  public List<Connection> removeConnections(String... globPatterns) {
    List<Connection> connectionsToRemove = getConnections(globPatterns);
    for (Connection connectionToRemove : connectionsToRemove) {
      connections
        .remove(connectionToRemove.getName())
        .close();
    }
    return connectionsToRemove;
  }

  /**
   * Removes all databases
   *
   * @return the deleted connections
   */
  @SuppressWarnings("UnusedReturnValue")
  public List<Connection> removeAllConnections() {

    List<Connection> connectionsRemoved = new ArrayList<>(this.connections.values());
    connections = new HashMap<>();
    return connectionsRemoved;

  }

  /**
   * @return all databases
   */
  public List<Connection> getConnections() {
    return new ArrayList<>(connections.values());
  }

  /**
   * @param globPatterns the glob patterns
   * @return all databases that match this glob patterns
   */
  public List<Connection> getConnections(String... globPatterns) {
    return this.connections.values()
      .stream()
      .filter(ds -> Arrays.stream(globPatterns)
        .anyMatch(gp -> Glob.createOf(gp).matches(ds.getName()))
      )
      .collect(Collectors.toList());
  }

  /**
   * Load the {@link ConnectionHowTos how-to connections}
   */
  public ConnectionVault loadHowtoConnections() {

    for (Connection connection : this.tabular.getHowtoConnections().values()) {
      connections.put(connection.getName(), connection);
    }

    return this;

  }

  /**
   * Build the connections variable from the connection vault file
   */
  private void load(Path path) {


    LOGGER.info("Opening the connection vault (" + path.toAbsolutePath() + ")");

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") Ini ini;
    try {
      ini = new Ini(path.toFile());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    for (String connectionName : ini.keySet()) {
      Wini.Section iniSection = ini.get(connectionName);

      /**
       Url Property search
       */

      /**
       * URI is a variable because it needs
       * templating and may be encryption feature
       */
      Variable uri = null;
      Set<Variable> variableMap = new SetKeyIndependent<>();
      for (String propertyName : iniSection.keySet()) {

        String value = iniSection.get(propertyName);
        ConnectionAttribute connectionAttribute = null;
        try {
          connectionAttribute = Casts.cast(propertyName, ConnectionAttribute.class);
        } catch (Exception e) {
          // not a standard attribute
          // a specific connection attribute then
        }
        Variable variable;
        try {

          if (connectionAttribute == null) {
            variable = vault.createVariable(propertyName, value, Origin.CONF);
          } else {
            variable = vault.createVariable(connectionAttribute, value, Origin.CONF);
          }
          if (connectionAttribute == ConnectionAttribute.URI) {
            uri = variable;
          }
        } catch (Exception e) {
          throw new RuntimeException("An error has occurred while reading the variable " + propertyName + " for the connection (" + connectionName + "). Error: " + e.getMessage(), e);
        }
        variableMap.add(variable);

      }

      if (uri == null) {
        throw new RuntimeException("The uri is a mandatory variable and was not found for the connection (" + connectionName + ") in the connection vault (" + this + ")");
      }

      /**
       * Create the connection
       */
      Connection connection = Connection.createConnectionFromProviderOrDefault(this.tabular, connectionName, (String) uri.getValueOrDefaultOrNull());
      // variables map should be in the building of the connection
      // as they may be used for the default values
      connection.setVariables(variableMap);
      connection.addVariable(vault.createVariable(
          ConnectionAttribute.ORIGIN,
          ConnectionOrigin.CONF,
          Origin.RUNTIME)
        .setPlainValue(Origin.CONF)
      );
      connections.put(connectionName, connection);

    }

  }


  /**
   * @param name the connection name
   * @return the removed datastore
   */
  public Connection deleteConnection(String name) {

    Connection connection = this.connections.remove(name);
    if (connection == null) {
      throw new IllegalStateException("The database (" + name + ") is non existent and therefore cannot be removed.");
    }
    return connection;
  }

  public Path getPath() {
    return this.path;
  }

  public ConnectionVault deleteConnectionIfExists(String name) {

    if (exists(name)) {
      deleteConnection(name)
        .close();
    }
    return this;
  }

  /**
   * @param name the connection name
   * @return boolean
   */
  boolean exists(String name) {

    return connections.get(name) != null;

  }

  /**
   * Put (Add or overwrite existing Connection)
   */
  public ConnectionVault put(Connection connection) {
    if (connection.getUriAsVariable() == null) {
      throw new RuntimeException("A connection string (url) is mandatory to add a datastore, the data store (" + connection.getName() + ") does not have any.");
    }
    connections.put(connection.getName(), connection);
    return this;
  }

  /**
   * Add (Connection should not exist)
   */
  public ConnectionVault add(Connection connection) {

    assert connections.get(connection.getName()) == null : "The data store (" + connection.getName() + ") exists already and cannot be added. Use the put function instead";
    return put(connection);
  }


  @Override
  public void close() {

    // we would flush at close but don't do it anymore
    // because there is range case:
    //  - flush would still run even if there is an error while loading, deleting connection
    //
  }

  public Connection getConnection(String name) {
    return this.connections.get(name);
  }


  @Override
  public String toString() {
    return path.toAbsolutePath().toString();
  }
}
