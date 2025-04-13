package com.tabulify.tabli;

import com.tabulify.transfer.*;
import net.bytle.cli.*;
import com.tabulify.Tabular;
import com.tabulify.TabularAttributes;
import com.tabulify.connection.Connection;
import com.tabulify.memory.MemoryDataPath;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.ResourcePath;
import com.tabulify.spi.Tabulars;
import com.tabulify.stream.InsertStream;
import com.tabulify.uri.DataUri;
import net.bytle.exception.*;
import net.bytle.java.JavaEnvs;
import net.bytle.log.Log;
import net.bytle.log.Logs;
import net.bytle.regexp.Glob;
import net.bytle.timer.Timer;
import net.bytle.type.Casts;
import net.bytle.type.Key;
import net.bytle.type.Manifest;
import net.bytle.type.ManifestAttribute;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static com.tabulify.tabli.TabliLog.LOGGER_TABLI;
import static com.tabulify.tabli.TabliWords.*;


public class Tabli {


  public static void main(String[] args) {

    // Initiate the client helper
    CliCommand rootCommand = CliCommand.createRoot(TabliWords.CLI_NAME, args)
      .setDescription("Tabli, the tabulify command line data processing tool")
      .setHelpWord(TabliWords.HELP_FLAG)
      .setVersionWord(VERSION_FLAG);

    // A client command example
    rootCommand.addExample(
      "To load a csv file into the sqlite database, you would type:",
      CliUsage.CODE_BLOCK,
      TabliWords.CLI_NAME + " " + TabliWords.DATA_COMMAND + " " + TabliWords.TRANSFER_COMMAND + " data.csv @sqlite",
      CliUsage.CODE_BLOCK
    );

    /*
     Initiate the library of options
     */
    TabliWords.initLibrary(rootCommand);

    /*
     The options for all command
     */
    rootCommand.addProperty(TabliWords.CONF_VARIABLES_PATH_PROPERTY)
      .setDescription("The path to a variables file")
      .setValueName("path")
      .setShortName("-vf");

    rootCommand.addProperty(ENVIRONMENT)
      .setDescription("The name of the environment")
      .setValueName("env")
      .setShortName("-e");

    rootCommand.addFlag(TabliWords.HELP_FLAG)
      .setShortName("-h")
      .setDescription("Print this help");
    rootCommand.setHelpWord(TabliWords.HELP_FLAG);

    rootCommand.addFlag(VERSION_FLAG)
      .setShortName("-v")
      .setDescription("Print version information");

    rootCommand.addProperty(TabliWords.LOG_LEVEL_LONG_OPTION)
      .setShortName("-l")
      .setDescription("Set the log level")
      .setValueName("error|warning|tip|info|fine")
      .setDefaultValue("info");

    rootCommand.addProperty(CONNECTION_VAULT_PROPERTY)
      .setDescription("The path where a connection vault is located")
      .setValueName("path")
      .setShortName("-cv");

    rootCommand.addProperty(TabliWords.PASSPHRASE_PROPERTY)
      .setShortName("-pp")
      .setDescription("A passphrase (master password) to decrypt the encrypted values")
      .setValueName("passphrase");

    /*
     * Output options
     */
    String outputGroup = "Output Operation Options";
    rootCommand.addProperty(TabliWords.OUTPUT_DATA_URI)
      .setTypeAsProperty()
      .setShortName("-odu")
      .setValueName("outputDataUri")
      .setGroup(outputGroup)
      .setDescription("defines the output data uri for the feedback data (default: console)");

    rootCommand.addProperty(OUTPUT_TRANSFER_OPERATION_OPTION)
      .setTypeAsProperty()
      .setGroup(outputGroup)
      .setDescription("defines the output transfer operation (insert, update, merge, copy). Default to `copy` for a file system and `insert` for a database.")
      .setShortName("-oop")
      .setValueName("transferOperation");

    rootCommand.addProperty(OUTPUT_OPERATION_OPTION)
      .setTypeAsProperty()
      .setGroup(outputGroup)
      .setDescription("defines the data operations (replace, truncate) on an existing output resource before transfer.")
      .setShortName("-oo")
      .setValueName("dataOperation");

    rootCommand.addProperty(PROJECT_FILE)
      .setShortName("-pf")
      .setGroup("Project Options:")
      .setDescription("A file system path to a project file")
      .setValueName("path");


    /*
     * The first command (the module)
     */
    rootCommand.addChildCommand(TabliWords.DATA_COMMAND)
      .setDescription("Data operations against data resources (table, file, ...).");
    rootCommand.addChildCommand(TabliWords.CONNECTION_COMMAND)
      .setDescription("Management and configuration of the connections to systems.");
    rootCommand.addChildCommand(TabliWords.VARIABLE_COMMAND)
      .setDescription("Management and configuration of the " + TabliWords.CLI_NAME + " variables environment.");
    rootCommand.addChildCommand(TabliWords.VAULT_COMMAND)
      .setDescription("Encrypt and decrypt sensitive information");
    rootCommand.addChildCommand(TabliWords.FLOW_COMMAND)
      .setDescription("Execute Flow");
    rootCommand.addChildCommand(DIAGNOSTIC_COMMAND)
      .setDescription("Print diagnostic information");


    /*
     * Parse
     */
    CliParser cliParser = rootCommand.parse();


    /*
     * LogLevel
     */
    Logs.setLevel(Level.SEVERE);

    /*
     * Init the context object
     */
    final String passphrase = cliParser.getString(TabliWords.PASSPHRASE_PROPERTY);
    /*
     * Project home
     */
    Path projectFilePath = cliParser.getPath(PROJECT_FILE);

    /*
     * Set the connection vault
     * Passphrase first
     */
    Path commandLineConnectionVault = cliParser.getPath(CONNECTION_VAULT_PROPERTY);

    // Command line last (higher priority)
    Path confPath = cliParser.getPath(CONF_VARIABLES_PATH_PROPERTY);


    String executionEnvironment = cliParser.getString(ENVIRONMENT);

    try (Tabular tabular = Tabular.tabular(passphrase, projectFilePath, commandLineConnectionVault, confPath, executionEnvironment)) {

      /*
       * Check for the version
       * https://docs.oracle.com/javase/tutorial/deployment/jar/packageman.html
       */
      if (cliParser.getBoolean(VERSION_FLAG)) {
        Manifest manifest = null;
        try {
          manifest = Manifest.createFor(Tabli.class);
        } catch (NoManifestException e) {
          System.out.println("Version: dev");
          Tabli.exit(0);
        }
        DataPath tabularVersion = tabular.getAndCreateRandomMemoryDataPath()
          .setLogicalName("Version")
          .getOrCreateRelationDef()
          .addColumn("Name")
          .addColumn("Value")
          .addColumn("Description")
          .getDataPath();
        try (InsertStream inputStream = tabularVersion.getInsertStream()) {
          for (ManifestAttribute manifestAttribute : ManifestAttribute.values()) {
            if (manifestAttribute.isVersion()) {
              try {
                inputStream.insert(
                  Key.toUriName(manifestAttribute.toString()),
                  manifest.getAttribute(manifestAttribute),
                  manifestAttribute.getDescription()
                );
              } catch (NoValueException e) {
                // no value
              }
            }
          }
        }
        Tabulars.print(tabularVersion);
        Tabli.exit(0);
      }

      try {
        String logLevel = cliParser.getString(LOG_LEVEL_LONG_OPTION).toLowerCase();
        if (!cliParser.has(LOG_LEVEL_LONG_OPTION)) {
          try {
            logLevel = tabular.getVariable(TabularAttributes.LOG_LEVEL, String.class);
          } catch (NoVariableException | NoValueException e) {
            logLevel = "info";
          } catch (CastException e) {
            // a string should not give a cast error
            throw IllegalArgumentExceptions.createFromException(e);
          }
        }
        Level level;
        switch (logLevel) {
          case "error":
            level = Level.SEVERE;
            break;
          case "warning":
            level = Level.WARNING;
            break;
          case "tip":
            level = Log.TIP;
            break;
          case "info":
            level = Level.INFO;
            break;
          case "fine":
            level = Level.FINE;
            break;
          default:
            throw new IllegalArgumentException("The value (" + logLevel + ") is unknown for the property " + CliUsage.getPrintWord(rootCommand.getLocalWord(LOG_LEVEL_LONG_OPTION)));
        }

        Logs.setLevel(level);
        LOGGER_TABLI.info("The log level was set to " + level);

        LOGGER_TABLI.info("The following arguments were received  (" + String.join(",", args) + ")");
        LOGGER_TABLI.info("The current file system path is " + Paths.get(".").toAbsolutePath());

        LOGGER_TABLI.info("The default connection was set to: `" + tabular.getDefaultConnection().getName() + "`");

        if (tabular.isProjectRun()) {
          LOGGER_TABLI.info("The project environment is: `" + tabular.getProjectConfigurationFile().getEnvironment() + "`");
          LOGGER_TABLI.info("The project variable file is: `" + tabular.getProjectConfigurationFile().getVariablesPath().toAbsolutePath() + "`");
          LOGGER_TABLI.info("The project connection vault is: `" + tabular.getProjectConfigurationFile().getConnectionVaultPath().toAbsolutePath() + "`");
        } else {
          LOGGER_TABLI.info("No project was found.");
        }

        /*
          Timer
         */
        Timer cliTimer = Timer
          .createFromUuid()
          .start();

        List<DataPath> feedbackDataPaths = new ArrayList<>();

        /*
          Process the command
         */
        List<CliCommand> commands = cliParser.getFoundedChildCommands();
        if (commands.isEmpty()) {
          throw new IllegalArgumentException("A known command must be given");
        } else {

          for (CliCommand childCommand : commands) {
            LOGGER_TABLI.info("The command (" + childCommand + ") was found");
            switch (childCommand.getName()) {
              case TabliWords.CONNECTION_COMMAND:
                feedbackDataPaths = TabliConnection.run(tabular, childCommand);
                break;
              case TabliWords.DATA_COMMAND:
                feedbackDataPaths = TabliData.run(tabular, childCommand);
                break;
              case TabliWords.VARIABLE_COMMAND:
                feedbackDataPaths = TabliVariable.run(tabular, childCommand);
                break;
              case TabliWords.VAULT_COMMAND:
                feedbackDataPaths = TabliVault.run(tabular, childCommand);
                break;
              case TabliWords.FLOW_COMMAND:
                feedbackDataPaths = TabliFlow.run(tabular, childCommand);
                break;
              case TabliWords.DIAGNOSTIC_COMMAND:
                feedbackDataPaths = TabliDiagnostic.run(tabular, childCommand);
                break;
              default:
                throw new IllegalArgumentException("The sub-command (" + childCommand.getName() + ") is unknown for the command (" + CliUsage.getFullChainOfCommand(rootCommand) + ")");
            }
          }


          /**
           * Feedback Data Paths
           *
           */
          String outputDataUriOption = cliParser.getString(OUTPUT_DATA_URI);
          DataUri outputDataUri = null;
          if (outputDataUriOption != null) {
            outputDataUri = tabular.createDataUri(outputDataUriOption);
          }

          for (DataPath feedbackDataPath : feedbackDataPaths) {
            if (outputDataUri != null) {

              Connection targetConnection = outputDataUri.getConnection();

              String targetPath;
              try {
                targetPath = outputDataUri.getPath();
              } catch (NoPathFoundException e) {
                targetPath = feedbackDataPath.getRelativePath();
              }

              try {
                String outputDataUriPattern = outputDataUri.getPattern();
                if (Glob.containsBackReferencesCharacters(outputDataUriPattern)) {
                  ResourcePath sourceGlobExpression = feedbackDataPath.getConnection().createStringPath(feedbackDataPath.getRelativePath());
                  targetPath = sourceGlobExpression.replace(feedbackDataPath.getRelativePath(), outputDataUriPattern);
                }
              } catch (NoPatternFoundException e) {
                // no output data uri pattern
              }
              DataPath targetDataPath = targetConnection.getDataPath(targetPath);

              /**
               * Transfer
               * Create the properties
               */
              TransferProperties outputTransferProperties = TransferProperties.create();
              String outputTransferOperation = cliParser.getString(OUTPUT_TRANSFER_OPERATION_OPTION);
              if (outputTransferOperation != null) {
                outputTransferProperties.setOperation(TransferOperation.createFrom(outputTransferOperation));
              }
              @SuppressWarnings("DuplicatedCode") TransferResourceOperations[] targetOperations = cliParser.getStrings(OUTPUT_OPERATION_OPTION).stream()
                .map(op -> {
                  try {
                    return Casts.cast(op, TransferResourceOperations.class);
                  } catch (CastException e) {
                    throw IllegalArgumentExceptions.createForArgumentValue(op, OUTPUT_OPERATION_OPTION, TransferResourceOperations.class, e);
                  }
                })
                .toArray(TransferResourceOperations[]::new);
              outputTransferProperties.addTargetOperations(targetOperations);

              TransferListener outputTransferListener = TransferManager
                .create()
                .setTransferProperties(outputTransferProperties)
                .addTransfer(feedbackDataPath, targetDataPath)
                .run()
                .getTransferListeners()
                .get(0);

              if (outputTransferListener.getExitStatus() == 0) {
                String msg = outputTransferListener.getRowCount() + " records from The feedback data resource (" + feedbackDataPath + ") has been transferred to (" + targetDataPath + ") with the (" + outputTransferListener.getMethod() + ") method.";
                LOGGER_TABLI.info(msg);
                System.out.println();
                System.out.println(msg);
                System.out.println();
              } else {
                throw new RuntimeException("An error has been seen while transferring the feedback output. The message was: " + outputTransferListener.getErrorMessages());
              }

            } else {

              System.out.println();
              if (feedbackDataPath.getDescription() != null) {
                System.out.println(feedbackDataPath.getDescription());
              }
              Tabulars.print(feedbackDataPath);
              System.out.println();

            }
            /**
             * The {@link TabliDataPrint print command } does not
             * copy the data and returns the object to print
             * We drop only memory data path
             * <p>
             * We drop them because
             * they have for now a fix name
             * and we don't want to use the content again
             * in a next test (for instance)
             */
            if (feedbackDataPath instanceof MemoryDataPath) {
              Tabulars.drop(feedbackDataPath);
            }
          }
        }


        /**
         * Exit
         */
        if (tabular.getExitStatus() != 0) {
          LOGGER_TABLI.severe("Errors were seen");
          Tabli.exit(tabular.getExitStatus());
        }

        /**
         * Latency and bye
         */
        LOGGER_TABLI.info("Latency Time: " + cliTimer.getResponseTimeInString() + " (hour:minutes:seconds:milli)");
        LOGGER_TABLI.info("       Ie (" + cliTimer.getResponseTimeInMilliSeconds() + ") milliseconds");
        LOGGER_TABLI.info("Done. Bye !");
        LOGGER_TABLI.info("To not see the `info` log, you can set the log level to `tip` with `tabli conf set " + LOG_LEVEL_LONG_OPTION + " tip`");


      } catch (Exception e) {

        if (e instanceof HelpPrintedException) {

          LOGGER_TABLI.fine("Help printed.");
          LOGGER_TABLI.info("Done. Bye !");
          /**
           * Only For the test, we throw it to test it
           *
           **/
          if (Tabli.hasBuildFileInRunningDirectory()) {
            throw e;
          }

          return;

        }

        String message = e.getMessage();
        if (!tabular.isStrict()) {
          message += "\nTry to run in non-strict mode with the flag " + TabliWords.NOT_STRICT_FLAG;
        }

        /**
         * Catch not planned exception
         * and illegal argument one
         */
        LOGGER_TABLI.severe("Fatal Exception: " + message);

        /**
         * Bad argument, no data selection,
         * Object exists already
         * print the usage
         */
        if (e instanceof IllegalArgumentException) {
          CliUsage.print(CliTree.getActiveLeafCommand(rootCommand));
        }

        /**
         * In a development mode, we want to see the stack,
         * we then throw the exception again
         */
        if (throwFinalException()) {
          System.out.println(); // layout
          LOGGER_TABLI.severe("Stack Trace:");
          throw e;
        }

        /**
         * Always returns a +1 and let the
         * calling script or user handle it.
         */
        Tabli.exit(1);


      }

    }


  }

  private static void exit(int exitStatus) {
    if (JavaEnvs.isJUnitTest()) {
      if (exitStatus != 0) {
        throw new TabliExitStatusException(exitStatus);
      }
      return;
    }
    System.exit(exitStatus);
  }

  /**
   * Because we don't want to throw an error when using the cli or docrun
   * By checking if the gradle.kts file is on the current directory
   *
   * @return if true or false
   */
  public static boolean hasBuildFileInRunningDirectory() {
    /**
     * Not the same than {@link TabularAttributes.IS_DEV}
     * that checks if there is a `build` directory in the children
     */
    boolean buildFileFound = false;
    try (DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get("."))) {
      for (Path path : paths) {
        if (path.getFileName().toString().contains("gradle.kts")) {
          buildFileFound = true;
          break;
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return buildFileFound;

  }


  /**
   * @return true:
   * * if this is a dev mode, we throw all exceptions
   * * in fine level to debug
   * <p>
   * We throw
   * * for the tests (example: to check that a help was asked)
   * * and to see the stack
   */
  private static Boolean throwFinalException() {
    /**
     * The stack trace is given
     * when we are below a fine level
     * or
     * in dev mode
     */
    return Logs.getLevel().intValue() <= Level.FINE.intValue() || JavaEnvs.isDev(Tabli.class);
  }
}
