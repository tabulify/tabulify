package com.tabulify.tabul;

import com.tabulify.Tabular;
import com.tabulify.TabularAttributeEnum;
import com.tabulify.TabularExecEnv;
import com.tabulify.TabularLogLevel;
import com.tabulify.conf.JarManifest;
import com.tabulify.conf.JarManifestAttribute;
import com.tabulify.connection.Connection;
import com.tabulify.diff.DataDiffColumn;
import com.tabulify.flow.operation.DiffPipelineStepArgument;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.ResourcePath;
import com.tabulify.spi.Tabulars;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.Printer;
import com.tabulify.stream.PrinterPrintFormat;
import com.tabulify.transfer.*;
import com.tabulify.uri.DataUriNode;
import net.bytle.cli.*;
import net.bytle.exception.*;
import net.bytle.java.JavaEnvs;
import net.bytle.log.Logs;
import net.bytle.regexp.Glob;
import net.bytle.timer.Timer;
import net.bytle.type.Casts;
import net.bytle.type.Enums;
import net.bytle.type.time.DurationShort;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.tabulify.tabul.TabulLog.LOGGER_TABUL;
import static com.tabulify.tabul.TabulWords.*;

/**
 * Tabul, the main entry
 * <p></p>
 * Why tabul?
 * Because tabul is an alternative form of a table
 * <a href="https://en.wiktionary.org/wiki/tabul">...</a>
 * and the term is pretty SEO free while tabli (tabulify cli) is not
 */
public class Tabul {


  public static void main(String[] args) {

    // Initiate the client helper
    CliCommand rootCommand = CliCommand.createRoot(TabulWords.CLI_NAME, args)
      .setDescription("Tabul, the tabulify command line data processing tool")
      .setHelpWord(TabulWords.HELP_FLAG)
      .setVersionWord(VERSION_FLAG);

    // A client command example
    rootCommand.addExample(
      "To load a csv file into the sqlite database, you would type:",
      CliUsage.CODE_BLOCK,
      TabulWords.CLI_NAME + " " + TabulWords.DATA_COMMAND + " " + TabulWords.TRANSFER_COMMAND + " data.csv @sqlite",
      CliUsage.CODE_BLOCK
    );

    /*
     Initiate the library of options
     */
    TabulWords.initLibrary(rootCommand);

    /*
     The options for all command
     */
    rootCommand.addProperty(TabulWords.CONF_PATH_PROPERTY)
      .setDescription("The path to a configuration file")
      .setValueName("path")
      .setShortName("-vf");

    rootCommand.addProperty(EXECUTION_ENVIRONMENT)
      .setDescription("The execution environment (prod or dev)")
      .setValueName("name")
      .setShortName("-ee");

    rootCommand.addFlag(TabulWords.HELP_FLAG)
      .setShortName("-h")
      .setDescription("Print this help");
    rootCommand.setHelpWord(TabulWords.HELP_FLAG);

    rootCommand.addFlag(VERSION_FLAG)
      .setShortName("-v")
      .setDescription("Print version information");

    rootCommand.addProperty(TabulWords.LOG_LEVEL_LONG_OPTION)
      .setShortName("-l")
      .setDescription("Set the log level")
      .setValueName("error|warning|tip|info|fine");

    rootCommand.addProperty(TabulWords.PASSPHRASE_PROPERTY)
      .setShortName("-pp")
      .setDescription("A passphrase (master password) to decrypt the encrypted vault values (Env: TABUL_PASSPHRASE)")
      .setValueName("passphrase");

    rootCommand.addFlag(TabulWords.NOT_STRICT_EXECUTION_FLAG);

    /*
     * Output options
     */
    String outputGroup = "Output Operation Options";
    rootCommand.addProperty(TabulWords.OUTPUT_DATA_URI)
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

    rootCommand.addProperty(APP_HOME)
      .setShortName("-ah")
      .setGroup("App Home")
      .setDescription(TabularAttributeEnum.APP_HOME.getDescription())
      .setValueName("path");

    rootCommand.addFlag(PIPE_MODE)
      .setDescription("Use pipe mode if you want to pipe the output in a shell. Pipe mode will not print the headers (ie column name) and will not make the control character visible");


    /*
     * The first command (the module)
     */
    rootCommand.addChildCommand(TabulWords.DATA_COMMAND)
      .setDescription("Data operations against data resources (table, file, ...).");
    rootCommand.addChildCommand(TabulWords.CONNECTION_COMMAND)
      .setDescription("Management and configuration of the connections to systems.");
    rootCommand.addChildCommand(TabulWords.APP_COMMAND)
      .setDescription("Management apps");
    rootCommand.addChildCommand(TabulWords.ENV_COMMAND)
      .setDescription("Management and configuration of the " + TabulWords.CLI_NAME + " execution environment");
    rootCommand.addChildCommand(TabulWords.VAULT_COMMAND)
      .setDescription("Encrypt and decrypt sensitive information");
    rootCommand.addChildCommand(TabulWords.FLOW_COMMAND)
      .setDescription("Execute Flows");
    rootCommand.addChildCommand(SERVICE_COMMAND)
      .setDescription("Start and stop services");


    /*
     * LogLevel
     */
    Logs.setLevel(Level.SEVERE);


    /**
     * Parse
     * May throw a {@link net.bytle.cli.HelpPrintedException}
     */
    CliParser cliParser;
    try {
      cliParser = rootCommand.parse();
    } catch (HelpPrintedException e) {
      handleHelpPrintedException(e);
      return;
    }


    /*
     * Init the context object
     */
    final String passphrase = cliParser.getString(TabulWords.PASSPHRASE_PROPERTY);
    /*
     * Project home
     */
    Path appHome = cliParser.getPath(APP_HOME);

    /*
     * Set the connection vault
     * Passphrase first
     */

    // Command line last (higher priority)
    Path confPath = cliParser.getPath(CONF_PATH_PROPERTY);


    String executionEnvironment = cliParser.getString(EXECUTION_ENVIRONMENT);
    TabularExecEnv execEnv = null;
    if (executionEnvironment != null) {
      try {
        execEnv = Casts.cast(executionEnvironment, TabularExecEnv.class);
      } catch (CastException e) {
        throw new IllegalArgumentException("The option (" + EXECUTION_ENVIRONMENT + ") has a env value (" + executionEnvironment + ") that is unknown. Possible values: " + Enums.toConstantAsStringCommaSeparated(TabularExecEnv.class), e);
      }
    }

    TabularLogLevel logLevel = null;
    String logLevelArg = cliParser.getString(LOG_LEVEL_LONG_OPTION);
    if (logLevelArg != null) {
      try {
        logLevel = Casts.cast(logLevelArg, TabularLogLevel.class);
      } catch (CastException e) {
        throw new RuntimeException("The value (" + logLevelArg + ") is not a valid logLevel. Valid value: " + Enums.toConstantAsStringCommaSeparated(TabularLogLevel.class), e);
      }
    }

    final Boolean isStrict = !cliParser.getBoolean(NOT_STRICT_EXECUTION_FLAG);
    try (Tabular tabular = Tabular.builder()
      .setPassphrase(passphrase)
      .setAppHome(appHome)
      .setConf(confPath)
      .setExecEnv(execEnv)
      .setLogLevel(logLevel)
      .setStrictExecution(isStrict)
      .build()
    ) {

      LOGGER_TABUL.info("The following arguments were received  (" + String.join(",", args) + ")");
      LOGGER_TABUL.info("The current file system path is " + Paths.get(".").toAbsolutePath());
      LOGGER_TABUL.info("The default connection was set to: `" + tabular.getDefaultConnection().getName() + "`");
      if (!tabular.isProjectRun()) {
        LOGGER_TABUL.info("No project was found.");
      }


      /**
       * An inner catch block because we need access to the strict parameters
       */
      try {

        /*
         * Check for the version
         * https://docs.oracle.com/javase/tutorial/deployment/jar/packageman.html
         */
        if (cliParser.getBoolean(VERSION_FLAG)) {
          JarManifest jarManifest = null;
          try {
            jarManifest = JarManifest.createFor(Tabul.class);
          } catch (NoManifestException e) {
            System.out.println("Version: dev");
            Tabul.exit(tabular, 0, null);
          }
          // for the compiler
          assert jarManifest != null;
          DataPath tabularVersion = tabular.getAndCreateRandomMemoryDataPath()
            .setLogicalName("Version")
            .getOrCreateRelationDef()
            .addColumn("Name")
            .addColumn("Value")
            .addColumn("Description")
            .getDataPath();
          try (InsertStream inputStream = tabularVersion.getInsertStream()) {
            for (JarManifestAttribute jarManifestAttribute : JarManifestAttribute.values()) {
              if (jarManifestAttribute.isVersion()) {
                try {
                  inputStream.insert(
                    tabular.toPublicName(jarManifestAttribute.toString()),
                    jarManifest.getAttribute(jarManifestAttribute),
                    jarManifestAttribute.getDescription()
                  );
                } catch (NoValueException e) {
                  // no value
                }
              }
            }
          }
          Tabulars.print(tabularVersion);
          Tabul.exit(tabular, 0, null);
        }


        /*
          Timer
         */
        Timer cliTimer = Timer
          .createFromUuid()
          .start();

        List<DataPath> feedbackDataPaths;
        /*
          Process the command
         */
        List<CliCommand> commands = cliParser.getFoundedChildCommands();
        if (commands.isEmpty()) {
          throw new IllegalArgumentException("A known command must be given");
        }

        for (CliCommand childCommand : commands) {
          LOGGER_TABUL.info("The command (" + childCommand + ") was found");
          switch (childCommand.getName()) {
            case TabulWords.CONNECTION_COMMAND:
              feedbackDataPaths = TabulConnection.run(tabular, childCommand);
              break;
            case TabulWords.APP_COMMAND:
              feedbackDataPaths = TabulApp.run(tabular, childCommand);
              break;
            case TabulWords.DATA_COMMAND:
              feedbackDataPaths = TabulData.run(tabular, childCommand);
              break;
            case TabulWords.ENV_COMMAND:
              feedbackDataPaths = TabulEnv.run(tabular, childCommand);
              break;
            case TabulWords.VAULT_COMMAND:
              feedbackDataPaths = TabulVault.run(tabular, childCommand);
              break;
            case TabulWords.FLOW_COMMAND:
              feedbackDataPaths = TabulFlow.run(tabular, childCommand);
              break;
            case TabulWords.SERVICE_COMMAND:
              feedbackDataPaths = TabulService.run(tabular, childCommand);
              break;
            default:
              throw new IllegalArgumentException("The sub-command (" + childCommand.getName() + ") is unknown for the command (" + CliUsage.getFullChainOfCommand(rootCommand) + ")");
          }

          /**
           * Feedback Data Paths
           */
          String outputDataUriOption = cliParser.getString(OUTPUT_DATA_URI);
          DataUriNode outputDataUri = null;
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
                targetPath = feedbackDataPath.getCompactPath();
              }

              try {
                String outputDataUriPattern = outputDataUri.getPath();
                if (Glob.containsBackReferencesCharacters(outputDataUriPattern)) {
                  ResourcePath sourceGlobExpression = feedbackDataPath.getConnection().createStringPath(feedbackDataPath.getCompactPath());
                  targetPath = sourceGlobExpression.replace(feedbackDataPath.getCompactPath(), outputDataUriPattern);
                }
              } catch (NoPathFoundException e) {
                // no path
              }
              DataPath targetDataPath = targetConnection.getDataPath(targetPath);

              /**
               * Transfer
               * Create the properties
               */
              TransferPropertiesSystem.TransferPropertiesSystemBuilder outputTransferProperties = TransferPropertiesSystem.builder();
              String outputTransferOperation = cliParser.getString(OUTPUT_TRANSFER_OPERATION_OPTION);
              if (outputTransferOperation != null) {
                outputTransferProperties.setOperation(TransferOperation.createFrom(outputTransferOperation));
              }
              TransferResourceOperations[] targetOperations = cliParser.getStrings(OUTPUT_OPERATION_OPTION).stream()
                .map(op -> {
                  try {
                    return Casts.cast(op, TransferResourceOperations.class);
                  } catch (CastException e) {
                    throw IllegalArgumentExceptions.createForArgumentValue(op, OUTPUT_OPERATION_OPTION, TransferResourceOperations.class, e);
                  }
                })
                .toArray(TransferResourceOperations[]::new);
              outputTransferProperties
                .setTargetOperations(targetOperations)
                .setOperation(TransferOperation.INSERT);

              TransferListener outputTransferListener = TransferManager
                .builder()
                .setTransferPropertiesSystem(outputTransferProperties)
                .build()
                .createOrder(feedbackDataPath, targetDataPath)
                .execute()
                .getTransferListeners()
                .get(0);

              if (outputTransferListener.getExitStatus() == 0) {
                String msg = outputTransferListener.getRowCount() + " records from The feedback data resource (" + feedbackDataPath + ") has been transferred to (" + targetDataPath + ") with the (" + outputTransferListener.getMethod() + ") method.";
                LOGGER_TABUL.info(msg);
                System.out.println();
                System.out.println(msg);
                System.out.println();
              } else {
                throw new RuntimeException("An error has been seen while transferring the feedback output. The message was: " + outputTransferListener.getErrorMessages());
              }

            } else {

              System.out.println();

              Printer.PrintBuilder printerBuilder = Printer.builder();
              Boolean isPipeModePresent = cliParser.getBoolean(PIPE_MODE);
              if (isPipeModePresent) {
                printerBuilder.setFormat(PrinterPrintFormat.PIPE);
              }

              /**
               * Colors?
               */
              try {
                String defaultValue = (String) DiffPipelineStepArgument.REPORT_DIFF_COLUMN_PREFIX.getDefaultValue();
                String colorsColumn = DataDiffColumn.COLORS.toKeyNormalizer().toSqlCase();
                String highlightColumnName = defaultValue + colorsColumn;
                // throw if no column found
                feedbackDataPath.getOrCreateRelationDef().getColumnDef(highlightColumnName);
                printerBuilder
                  .setColorsColumnName(highlightColumnName);
              } catch (NoColumnException e) {
                //
              }

              /**
               * Printer
               */
              printerBuilder
                .build()
                .print(feedbackDataPath);

              System.out.println();

            }
          }
        }


        /**
         * Exit
         */
        if (tabular.getExitStatus() != 0) {
          LOGGER_TABUL.severe("Errors were seen");
          Tabul.exit(tabular, tabular.getExitStatus(), null);
        }

        /**
         * Latency and bye
         */
        LOGGER_TABUL.info("Latency Time: " + DurationShort.create(cliTimer.getDuration()).toIsoDuration());
        LOGGER_TABUL.info("Done. Bye !");
        LOGGER_TABUL.info("To not see the `info` log, you can set the log level to `tip` with `" + CLI_NAME + " " + ENV_COMMAND + " set " + LOG_LEVEL_LONG_OPTION + " tip`");


      } catch (Exception e) {


        if (e instanceof TabulExitStatusException) {

          int exitStatus = ((TabulExitStatusException) e).getExitStatus();
          if (exitStatus == 0) {
            /**
             * An earlier {@link Tabul#exit(int)}
             * may have created it, we just return
             */
            return;
          }

        }

        if (e instanceof HelpPrintedException) {
          handleHelpPrintedException((HelpPrintedException) e);
          return;
        }


        String message = e.getMessage();
        if (!tabular.isStrictExecution()) {
          message += "\nTry to run in non-strict mode with the flag " + TabulWords.NOT_STRICT_EXECUTION_FLAG;
        }


        /**
         * Bad argument, no data selection,
         * Object exists already
         * print the usage
         */
        if (e instanceof IllegalArgumentException) {

          /**
           * Arguments parsing error?
           */
          String originErrorFileName = e.getStackTrace()[0].getFileName();
          if (originErrorFileName != null && originErrorFileName.contains("CliParser")) {
            CliUsage.print(CliTree.getActiveLeafCommand(rootCommand));
          }

          LOGGER_TABUL.severe(message);

          Tabul.exit(tabular, 1, e);
          return;
        }

        /**
         * Catch not planned exception
         */
        LOGGER_TABUL.severe("Fatal Exception: " + message);
        System.err.println(); // layout
        LOGGER_TABUL.severe("Stack Trace:");
        throw e;

      }

    }


  }

  /**
   * Catch Help Exception
   */
  private static boolean handleHelpPrintedException(HelpPrintedException e) {


    LOGGER_TABUL.fine("Help printed.");
    LOGGER_TABUL.info("Done. Bye !");
    /**
     * Only For the test, we throw it to test it
     **/
    if (JavaEnvs.isJUnitTest()) {
      throw e;
    }

    return true;


  }

  private static void exit(Tabular tabular, int exitStatus, Exception e) {
    if (tabular.isIdeEnv()) {
      // We need to throw to exit
      // The code does not use a return
      throw new TabulExitStatusException(exitStatus, e);
    }
    // Print stack trace on fine level
    if (exitStatus != 0 && e != null && tabular.getLogLevel().intValue() >= Level.FINE.intValue()) {
      LOGGER_TABUL.severe("Stack Trace:");
      //noinspection CallToPrintStackTrace
      e.printStackTrace();
    }
    System.exit(exitStatus);
  }


  static void printEmptySelectionFeedback(List<DataUriNode> dataSelectors) {
    System.out.println();
    System.out.println("No data resources selected by the data selectors (" + dataSelectors.stream().sorted().map(DataUriNode::toString).collect(Collectors.joining(", ")) + ")");
    System.out.println();
  }
}
