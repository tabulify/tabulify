package net.bytle.db.connection;


import net.bytle.db.DbLoggers;
import net.bytle.db.Tabular;
import net.bytle.exception.InternalException;
import net.bytle.exception.NoValueException;
import net.bytle.fs.Fs;
import net.bytle.log.Log;
import net.bytle.regexp.Glob;
import net.bytle.type.Casts;
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
 * A database store implementation based on ini file
 * If a password is saved a passphrase should be provided
 */
public class ConnectionVault implements AutoCloseable {


  protected static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;


  private final Tabular tabular;

  private final Path path;


  /**
   * The in-memory connectionVaultStorage
   * When the init has been done, the value is not null anymore
   */
  private Map<String, Connection> connections = null;


  public ConnectionVault(Tabular tabular, Path path) {

    this.tabular = tabular;
    Objects.requireNonNull(path);
    this.path = path;

  }

  public static ConnectionVault create(Tabular tabular, Path connectionVaultPath) {
    return new ConnectionVault(tabular, connectionVaultPath);
  }


  /**
   * Write the changes to the disk
   */
  public void flush() {
    if (this.connections == null) {
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
      Fs.createFile(tempFile);
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
        for (Variable variable : connection.getVariables().stream().sorted().collect(Collectors.toList())) {
          if (variable.getAttribute() == ConnectionAttribute.NAME) {
            continue;
          }
          String valueToStore;
          if (variable.getAttribute() == ConnectionAttribute.URI) {
            uriFound = true;
            valueToStore = (String) variable.getOriginalValue();
            if (valueToStore == null) {
              try {
                valueToStore = (String) variable.getValueOrDefault();
              } catch (NoValueException e) {
                throw new InternalException("The URI variable has no value for the connection (" + connection + ")");
              }
            }
          } else {
            Object originalValue = variable.getOriginalValue();
            if (originalValue == null) {
              continue;
            }
            valueToStore = originalValue.toString();
          }
          ini.put(connectionNameSection, variable.getPublicName(), valueToStore);
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
   * @return the howto datastores
   */
  List<Connection> getHowToConnections() {
    return ConnectionHowTos.getDataStores(this.tabular);
  }


  /**
   * Remove all connections metadata information that matches one of the globPatterns
   *
   * @param globPatterns one or more glob pattern
   * @return a list of database name removed
   */
  public List<Connection> removeConnections(String... globPatterns) {
    initCheck();
    List<Connection> connectionsToRemove = getConnections(globPatterns);
    for (Connection connectionToRemove : connectionsToRemove) {
      connections.remove(connectionToRemove.getName());
    }
    return connectionsToRemove;
  }

  /**
   * Removes all databases
   *
   * @return the deleted connections
   */
  public List<Connection> removeAllConnections() {
    initCheck();
    List<Connection> connectionsRemoved = new ArrayList<>(this.connections.values());
    connections = new HashMap<>();
    return connectionsRemoved;

  }

  /**
   * @return all databases
   */
  public List<Connection> getConnections() {
    initCheck();
    return new ArrayList<>(connections.values());
  }

  /**
   * @param globPatterns the glob patterns
   * @return all databases that match this glob patterns
   */
  public List<Connection> getConnections(String... globPatterns) {
    initCheck();
    return this.connections.values()
      .stream()
      .filter(ds -> Arrays.stream(globPatterns)
        .anyMatch(gp -> Glob.createOf(gp).matches(ds.getName()))
      )
      .collect(Collectors.toList());
  }


  /**
   * Build the connections variable from the connection vault file
   * or if empty build with the {@link ConnectionHowTos}
   */
  public ConnectionVault init() {

    if (this.connections != null) {
      throw new RuntimeException("The connection vault has already been initialized");
    }
    this.connections = new HashMap<>();

    if (!Files.exists(this.path) && !this.tabular.isProjectRun()) {

      LOGGER.info("The connection vault file (" + this.path + ") does not exist. Creating it with the `howtos` datastores");
      /**
       * Create the vault with the how to connections
       */
      connections = getHowToConnections()
        .stream()
        .collect(Collectors.toMap(Connection::getName, Connection::of));

      this.flush();
      LOGGER.info("The connection vault file (" + this.path + ") was created.");

    } else {

      LOGGER.info("Opening the connection vault (" + path.toAbsolutePath() + ")");
      /**
       * Connection Vault Read
       */
      @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") Ini ini;
      try {
        ini = new Ini(this.path.toFile());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      for (String connectionName : ini.keySet()) {
        Wini.Section iniSection = ini.get(connectionName);

        /**
         * Url Property search
         */

        /**
         *
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
              variable = tabular.getVault().createVariable(propertyName, value);
            } else {
              variable = tabular.getVault().createVariable(connectionAttribute, value);
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
        connections.put(connectionName, connection);

      }
    }
    return this;
  }


  /**
   * @param name the connection name
   * @return the removed datastore
   */
  public Connection deleteConnection(String name) {
    initCheck();
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
    initCheck();
    if (exists(name)) {
      deleteConnection(name);
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

  public ConnectionVault add(Connection connection) {
    initCheck();
    assert connections.get(connection.getName()) == null : "The data store (" + connection.getName() + ") exists already and cannot be added";
    if (connection.getUriAsVariable() == null) {
      throw new RuntimeException("A connection string (url) is mandatory to add a datastore, the data store (" + connection.getName() + ") does not have any.");
    }
    connections.put(connection.getName(), connection);
    return this;
  }


  @Override
  public void close() {

    // we would flush at close but don't do it anymore
    // because there is range case:
    //  - flush would still run even if there is an error while loading, deleting connection
    //
  }

  public Connection getConnection(String name) {
    initCheck();
    return this.connections.get(name);
  }

  /**
   * The init is done lazily because not every command
   * needs to get connection
   * <p>
   * ie encryption is a good example, if you encrypt with another passphrase
   * the {@link ConnectionHowTos} will be encrypted with this passphrase ...
   **/
  private void initCheck() {
    if (this.connections == null) {
      init();
    }
  }

  @Override
  public String toString() {
    return path.toAbsolutePath().toString();
  }
}
